package com.dapcode.backend_core.service.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import com.dapcode.backend_core.dto.submission.SubmissionResponse;
import com.dapcode.backend_core.dto.submission.SubmitCodeRequest;
import com.dapcode.backend_core.entity.Problem;
import com.dapcode.backend_core.entity.Submission;
import com.dapcode.backend_core.entity.TestCase;
import com.dapcode.backend_core.judge0.Judge0Client;
import com.dapcode.backend_core.judge0.Judge0Response;
import com.dapcode.backend_core.judge0.Judge0Status;
import com.dapcode.backend_core.judge0.Judge0SubmissionRequest;
import com.dapcode.backend_core.repository.ProblemRepository;
import com.dapcode.backend_core.repository.SubmissionRepository;
import com.dapcode.backend_core.repository.TestCaseRepository;
import com.dapcode.backend_core.repository.UserRepository;

class BatchExecutionTest {

    private final CodeHarnessServiceImpl harness = new CodeHarnessServiceImpl();

    private String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        is.transferTo(bos);
        return bos.toString(StandardCharsets.UTF_8);
    }

    private TestCase tc(String in, String out) {
        return TestCase.builder().id(UUID.randomUUID()).inputData(in).expectedOutput(out).isHidden(false).build();
    }

    @Test
    @DisplayName("Java Standard: 1 lần javac + 1 lần chạy chấm trọn 5 test cases (kể cả 1 case ném exception mà không làm hỏng case sau)")
    void testJavaStandardSingleCompileBatch(@TempDir Path tempDir) throws Exception {
        String javaCode = """
                class Solution {
                    int leaked = 0;
                    public int[] twoSum(int[] nums, int target) {
                        leaked++;
                        if (nums.length == 0) throw new IllegalArgumentException("empty");
                        Map<Integer, Integer> map = new HashMap<>();
                        for (int i = 0; i < nums.length; i++) {
                            int need = target - nums[i];
                            if (map.containsKey(need)) return new int[]{map.get(need), i};
                            map.put(nums[i], i);
                        }
                        return new int[]{};
                    }
                }
                """;
        List<TestCase> cases = List.of(
                tc("nums = [2,7,11,15], target = 9", "[0, 1]"),
                tc("nums = [3,2,4], target = 6", "[1, 2]"),
                tc("nums = [], target = 0", "[]"),
                tc("nums = [3,3], target = 6", "[0, 1]"),
                tc("nums = [1,5,9], target = 10", "[0, 2]")
        );

        String wrapped = harness.prepareSourceCode("two-sum", 62, javaCode);
        Path mainJava = tempDir.resolve("Main.java");
        Files.writeString(mainJava, wrapped, StandardCharsets.UTF_8);

        Process compile = new ProcessBuilder("javac", mainJava.toString())
                .directory(tempDir.toFile()).redirectErrorStream(true).start();
        String cOut = readAll(compile.getInputStream());
        assertTrue(compile.waitFor(20, TimeUnit.SECONDS));
        assertEquals(0, compile.exitValue(), cOut);

        String batchStdin = BatchExecutionProtocol.encodeBatchInput(cases);
        Process run = new ProcessBuilder("java", "-cp", tempDir.toString(), "Main")
                .directory(tempDir.toFile()).start();
        try (OutputStream os = run.getOutputStream()) {
            os.write(batchStdin.getBytes(StandardCharsets.UTF_8));
        }
        String stdout = readAll(run.getInputStream());
        String stderr = readAll(run.getErrorStream());
        assertTrue(run.waitFor(10, TimeUnit.SECONDS));
        assertEquals(0, run.exitValue(), stderr);

        List<BatchExecutionProtocol.CaseOutcome> outcomes = BatchExecutionProtocol.parseBatchOutput(stdout, cases.size());
        assertNotNull(outcomes, "Failed to parse batch stdout:\n" + stdout);
        assertEquals(5, outcomes.size());

        assertTrue(outcomes.get(0).ok());
        assertEquals("[0, 1]", outcomes.get(0).payload());
        assertTrue(outcomes.get(1).ok());
        assertEquals("[1, 2]", outcomes.get(1).payload());
        assertFalse(outcomes.get(2).ok());
        assertTrue(outcomes.get(2).payload().contains("IllegalArgumentException"));
        assertTrue(outcomes.get(3).ok());
        assertEquals("[0, 1]", outcomes.get(3).payload());
        assertTrue(outcomes.get(4).ok());
        assertEquals("[0, 2]", outcomes.get(4).payload());
    }

    @Test
    @DisplayName("Java Design (TimeMap): Chạy nhiều test case độc lập trong cùng 1 tiến trình không rò rỉ trạng thái")
    void testJavaDesignSingleCompileBatch(@TempDir Path tempDir) throws Exception {
        String code = """
                class TimeMap {
                    Map<String, List<Pair<String, Integer>>> map = new HashMap<>();
                    public TimeMap() {}
                    public void set(String key, String value, int timestamp) {
                        map.computeIfAbsent(key, k -> new ArrayList<>()).add(new Pair<>(value, timestamp));
                    }
                    public String get(String key, int timestamp) {
                        List<Pair<String, Integer>> list = map.get(key);
                        if (list == null) return "";
                        for (int i = list.size() - 1; i >= 0; i--) {
                            if (list.get(i).getValue() <= timestamp) return list.get(i).getKey();
                        }
                        return "";
                    }
                }
                """;
        List<TestCase> cases = List.of(
                tc("[\"TimeMap\", \"set\", \"get\"]\n[[], [\"a\", \"v1\", 1], [\"a\", 1]]", "[null, null, \"v1\"]"),
                tc("[\"TimeMap\", \"get\"]\n[[], [\"a\", 1]]", "[null, \"\"]")
        );
        String wrapped = harness.prepareSourceCode("time-based-key-value-store", 62, code);
        Path mainJava = tempDir.resolve("Main.java");
        Files.writeString(mainJava, wrapped, StandardCharsets.UTF_8);

        Process compile = new ProcessBuilder("javac", mainJava.toString())
                .directory(tempDir.toFile()).redirectErrorStream(true).start();
        assertTrue(compile.waitFor(20, TimeUnit.SECONDS));
        assertEquals(0, compile.exitValue(), readAll(compile.getInputStream()));

        Process run = new ProcessBuilder("java", "-cp", tempDir.toString(), "Main")
                .directory(tempDir.toFile()).start();
        try (OutputStream os = run.getOutputStream()) {
            os.write(BatchExecutionProtocol.encodeBatchInput(cases).getBytes(StandardCharsets.UTF_8));
        }
        String stdout = readAll(run.getInputStream());
        assertTrue(run.waitFor(10, TimeUnit.SECONDS));
        assertEquals(0, run.exitValue(), readAll(run.getErrorStream()));

        List<BatchExecutionProtocol.CaseOutcome> outcomes = BatchExecutionProtocol.parseBatchOutput(stdout, 2);
        assertNotNull(outcomes);
        assertEquals("[null, null, \"v1\"]", outcomes.get(0).payload());
        assertEquals("[null, \"\"]", outcomes.get(1).payload());
    }

    @Test
    @DisplayName("Python: Chạy trọn 3 test cases trong 1 tiến trình duy nhất (gồm 1 case lỗi runtime)")
    void testPythonSingleProcessBatch(@TempDir Path tempDir) throws Exception {
        String pyCode = """
                class Solution:
                    def divTen(self, x: int) -> int:
                        return 10 // x
                """;
        List<TestCase> cases = List.of(
                tc("x = 2", "5"),
                tc("x = 0", "0"),
                tc("x = 5", "2")
        );
        String wrapped = harness.prepareSourceCode("div-ten", 71, pyCode);
        Path script = tempDir.resolve("sol.py");
        Files.writeString(script, wrapped, StandardCharsets.UTF_8);

        Process p = new ProcessBuilder("python", script.toString()).start();
        try (OutputStream os = p.getOutputStream()) {
            os.write(BatchExecutionProtocol.encodeBatchInput(cases).getBytes(StandardCharsets.UTF_8));
        }
        String stdout = readAll(p.getInputStream());
        assertTrue(p.waitFor(10, TimeUnit.SECONDS));
        assertEquals(0, p.exitValue(), readAll(p.getErrorStream()));

        List<BatchExecutionProtocol.CaseOutcome> outcomes = BatchExecutionProtocol.parseBatchOutput(stdout, 3);
        assertNotNull(outcomes, stdout);
        assertTrue(outcomes.get(0).ok());
        assertEquals("5", outcomes.get(0).payload());
        assertFalse(outcomes.get(1).ok());
        assertTrue(outcomes.get(1).payload().contains("ZeroDivisionError"));
        assertTrue(outcomes.get(2).ok());
        assertEquals("2", outcomes.get(2).payload());
    }

    @Test
    @DisplayName("C++: Sinh đúng khung batch dual-mode cho cả Standard và Design (reset cin.clear/cout.clear/state)")
    void testCppBatchWrapperGeneration() {
        String stdWrapped = harness.prepareSourceCode("two-sum", 54,
                "class Solution { public: vector<int> twoSum(vector<int>& nums, int target) { return {}; } };");
        assertTrue(stdWrapped.contains("#include <chrono>"));
        assertTrue(stdWrapped.contains("static void _dapcode_run_case()"));
        assertTrue(stdWrapped.contains("<<<DAPCODE_BATCH_V1>>>"));
        assertTrue(stdWrapped.contains("cin.clear();"));
        assertTrue(stdWrapped.contains("cout.clear();"));
        assertTrue(stdWrapped.contains("_POS_CYCLE = -1;"));
        assertTrue(stdWrapped.contains("_GUESS_PICK = 0;"));

        String designWrapped = harness.prepareSourceCode("lru-cache", 54,
                "class LRUCache { public: LRUCache(int cap) {} int get(int k) { return -1; } void put(int k, int v) {} };");
        assertTrue(designWrapped.contains("static void _dapcode_run_case()"));
        assertTrue(designWrapped.contains("<<<DAPCODE_BATCH_V1>>>"));

        assertTrue(harness.supportsBatchExecution(54, "class Solution {};"));
        assertFalse(harness.supportsBatchExecution(54, "int main() { return 0; }"));
        assertFalse(harness.supportsBatchExecution(62, "public class Main { public static void main(String[] a){} }"));
        assertFalse(harness.supportsBatchExecution(71, "if __name__ == '__main__': pass"));
    }

    @Test
    @DisplayName("SubmissionServiceImpl: Chỉ gửi 1 job Judge0 cho 15 test cases (ACCEPTED & CE), và tự fallback khi TLE")
    void testSubmissionServiceSingleCompileAndFallback() {
        ProblemRepository problemRepo = mock(ProblemRepository.class);
        TestCaseRepository tcRepo = mock(TestCaseRepository.class);
        SubmissionRepository subRepo = mock(SubmissionRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        Judge0Client judge0 = mock(Judge0Client.class);
        SubmissionStreamManager stream = mock(SubmissionStreamManager.class);

        Problem prob = Problem.builder()
                .id(UUID.randomUUID())
                .slug("valid-anagram")
                .difficulty("Easy")
                .timeLimitMs(2000)
                .memoryLimitKb(262144)
                .build();

        List<TestCase> cases = new ArrayList<>();
        StringBuilder fakeBatchStdout = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            cases.add(tc("s = \"ab\", t = \"ba\"", "True"));
            fakeBatchStdout.append("<<<DAPCODE_RES_START:").append(i).append(":OK:1.250>>>\n")
                    .append("True\n")
                    .append("<<<DAPCODE_RES_END:").append(i).append(">>>\n");
        }

        UUID subId = UUID.randomUUID();
        when(problemRepo.findBySlug("valid-anagram")).thenReturn(Optional.of(prob));
        when(tcRepo.findByProblemId(prob.getId())).thenReturn(cases);
        when(subRepo.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            if (s.getId() == null) s.setId(subId);
            return s;
        });
        when(subRepo.findById(subId)).thenAnswer(inv -> Optional.of(
                Submission.builder().id(subId).problem(prob).status("PENDING").build()));

        Judge0Response batchOk = new Judge0Response();
        Judge0Response.Status okSt = new Judge0Response.Status();
        okSt.setId(Judge0Status.ACCEPTED.getId());
        batchOk.setStatus(okSt);
        batchOk.setStdout(Base64.getEncoder().encodeToString(fakeBatchStdout.toString().getBytes(StandardCharsets.UTF_8)));
        batchOk.setMemory(4096);
        when(judge0.executeBatch(anyList())).thenReturn(List.of(batchOk));

        SubmissionServiceImpl service = new SubmissionServiceImpl(
                problemRepo, tcRepo, subRepo, userRepo, judge0, stream, harness);

        SubmitCodeRequest req = SubmitCodeRequest.builder()
                .problemSlug("valid-anagram")
                .languageId(54)
                .sourceCode("class Solution { public: bool isAnagram(string s, string t) { return true; } };")
                .build();

        SubmissionResponse res = service.submitAndEvaluate(req);
        assertEquals("ACCEPTED", res.getStatus());
        assertEquals(15, res.getPassedTestCases());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Judge0SubmissionRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(judge0, times(1)).executeBatch(captor.capture());
        assertEquals(1, captor.getValue().size(), "Phải chỉ gửi 1 job Judge0 duy nhất thay vì 15!");
        assertEquals("-std=c++17", captor.getValue().get(0).getCompiler_options());

        Judge0Client judge0Ce = mock(Judge0Client.class);
        Judge0Response ceRes = new Judge0Response();
        Judge0Response.Status ceSt = new Judge0Response.Status();
        ceSt.setId(Judge0Status.COMPILATION_ERROR.getId());
        ceRes.setStatus(ceSt);
        ceRes.setCompile_output(Base64.getEncoder().encodeToString("syntax error".getBytes(StandardCharsets.UTF_8)));
        when(judge0Ce.executeBatch(anyList())).thenReturn(List.of(ceRes));

        SubmissionServiceImpl serviceCe = new SubmissionServiceImpl(
                problemRepo, tcRepo, subRepo, userRepo, judge0Ce, stream, harness);
        SubmissionResponse resCe = serviceCe.submitAndEvaluate(req);
        assertEquals("COMPILATION_ERROR", resCe.getStatus());
        verify(judge0Ce, times(1)).executeBatch(anyList());

        Judge0Client judge0Tle = mock(Judge0Client.class);
        Judge0Response tleRes = new Judge0Response();
        Judge0Response.Status tleSt = new Judge0Response.Status();
        tleSt.setId(Judge0Status.TIME_LIMIT_EXCEEDED.getId());
        tleRes.setStatus(tleSt);
        when(judge0Tle.executeBatch(anyList())).thenAnswer(inv -> {
            List<Judge0SubmissionRequest> arg = inv.getArgument(0);
            if (arg.size() == 1) return List.of(tleRes);
            return arg.stream().map(r -> {
                Judge0Response item = new Judge0Response();
                item.setStatus(tleSt);
                return item;
            }).toList();
        });

        SubmissionServiceImpl serviceTle = new SubmissionServiceImpl(
                problemRepo, tcRepo, subRepo, userRepo, judge0Tle, stream, harness);
        SubmissionResponse resTle = serviceTle.submitAndEvaluate(req);
        assertEquals("TIME_LIMIT_EXCEEDED", resTle.getStatus());
        verify(judge0Tle, times(2)).executeBatch(anyList());
    }
}
