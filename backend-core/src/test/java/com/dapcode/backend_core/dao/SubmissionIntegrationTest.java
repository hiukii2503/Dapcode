package com.dapcode.backend_core.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.dapcode.backend_core.dto.submission.SubmissionResponse;
import com.dapcode.backend_core.dto.submission.SubmitCodeRequest;
import com.dapcode.backend_core.dto.submission.TestCaseResult;
import com.dapcode.backend_core.entity.Problem;
import com.dapcode.backend_core.entity.Submission;
import com.dapcode.backend_core.entity.TestCase;
import com.dapcode.backend_core.entity.Users;
import com.dapcode.backend_core.judge0.Judge0Client;
import com.dapcode.backend_core.judge0.Judge0Response;
import com.dapcode.backend_core.judge0.Judge0Status;
import com.dapcode.backend_core.repository.ProblemRepository;
import com.dapcode.backend_core.repository.SubmissionRepository;
import com.dapcode.backend_core.repository.TestCaseRepository;
import com.dapcode.backend_core.repository.UserRepository;
import com.dapcode.backend_core.service.submission.CodeHarnessService;
import com.dapcode.backend_core.service.submission.CodeHarnessServiceImpl;
import com.dapcode.backend_core.service.submission.SubmissionServiceImpl;
import com.dapcode.backend_core.service.submission.SubmissionStreamManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SubmissionIntegrationTest {

    private SubmissionDAO submissionDAO;
    private ProblemDAO problemDAO;
    private UserDAO userDAO;
    private TestCaseDAO testCaseDAO;
    private CodeHarnessService codeHarnessService;

    private Users testUser;
    private Problem testProblem;
    private TestCase publicCase;
    private TestCase hiddenCase;

    @BeforeEach
    void setUp() {
        submissionDAO = new SubmissionDAOImpl();
        problemDAO = new ProblemDAOImpl();
        userDAO = new UserDAOImpl();
        testCaseDAO = new TestCaseDAOImpl();
        codeHarnessService = new CodeHarnessServiceImpl();

        long ts = System.currentTimeMillis();
        testUser = userDAO.insert(Users.builder()
                .username("sub_tester_" + ts)
                .email("sub_tester_" + ts + "@dapcode.io")
                .passwordHash("$2a$10$dummyhashforintegrationtest")
                .role("ROLE_USER")
                .isActive(true)
                .xp(100)
                .streakCount(1)
                .build());

        testProblem = problemDAO.insert(Problem.builder()
                .title("Two Sum Integration " + ts)
                .slug("two-sum-it-" + ts)
                .difficulty("EASY")
                .category("Arrays")
                .descriptionMarkdown("Tìm 2 chỉ số có tổng bằng target.")
                .timeLimitMs(2000)
                .memoryLimitKb(262144)
                .isPublished(true)
                .build());

        publicCase = testCaseDAO.insert(TestCase.builder()
                .problem(testProblem)
                .inputData("[2,7,11,15]\n9")
                .expectedOutput("[0,1]")
                .isHidden(false)
                .build());

        hiddenCase = testCaseDAO.insert(TestCase.builder()
                .problem(testProblem)
                .inputData("[3,2,4]\n6")
                .expectedOutput("[1,2]")
                .isHidden(true)
                .build());

        log.info("[SETUP] Created test fixtures in DB: userId={}, problemId={}, slug='{}', publicCaseId={}, hiddenCaseId={}",
                testUser.getId(), testProblem.getId(), testProblem.getSlug(), publicCase.getId(), hiddenCase.getId());
    }

    @AfterEach
    void tearDown() {
        UUID deletedProblemId = testProblem != null ? testProblem.getId() : null;
        UUID deletedUserId = testUser != null ? testUser.getId() : null;
        if (deletedProblemId != null) {
            problemDAO.delete(deletedProblemId);
        }
        if (deletedUserId != null) {
            try (var conn = DBContext.getConnection();
                 var ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                ps.setObject(1, deletedUserId);
                ps.executeUpdate();
            } catch (Exception ignored) {
            }
        }
        log.info("[CLEANUP] Cleaned up test fixtures: problemId={}, userId={}", deletedProblemId, deletedUserId);
    }

    @Test
    @Order(1)
    @DisplayName("Kịch bản 1: Luồng nộp bài từ trạng thái PENDING sang ACCEPTED và cập nhật Runtime/Memory trên DB thật")
    void test1_SubmissionLifecycle_PendingToAccepted() {
        log.info("=== [Scenario 1] Starting submission lifecycle test (PENDING -> ACCEPTED) via HikariCP ===");
        Submission pendingSub = Submission.builder()
                .user(testUser)
                .problem(testProblem)
                .sourceCode("class Solution:\n    def twoSum(self, nums, target):\n        return [0,1]")
                .languageId(71)
                .status("PENDING")
                .build();

        Submission saved = submissionDAO.insert(pendingSub);
        assertNotNull(saved, "Lưu bài nộp trạng thái PENDING qua HikariCP phải thành công");
        assertEquals("PENDING", saved.getStatus());
        log.info("[INSERT] Saved new submission -> id={}, languageId={}, status='{}'",
                saved.getId(), saved.getLanguageId(), saved.getStatus());

        Submission updated = submissionDAO.updateStatusAndMetrics(saved.getId(), "ACCEPTED", 24.5, 14200);
        assertNotNull(updated);
        assertEquals("ACCEPTED", updated.getStatus());
        assertEquals(24.5, updated.getTotalRuntimeMs());
        assertEquals(14200, updated.getMaxMemoryKb());
        log.info("[UPDATE] Updated grading result -> id={}, status='{}', runtime={}ms, memory={}KB",
                updated.getId(), updated.getStatus(), updated.getTotalRuntimeMs(), updated.getMaxMemoryKb());

        Optional<Submission> fromDb = submissionDAO.findById(saved.getId());
        assertTrue(fromDb.isPresent());
        assertEquals("ACCEPTED", fromDb.get().getStatus());
        log.info("[VERIFY DB] Reloaded from PostgreSQL (findById) -> id={}, status='{}', runtime={}ms, memory={}KB",
                fromDb.get().getId(), fromDb.get().getStatus(),
                fromDb.get().getTotalRuntimeMs(), fromDb.get().getMaxMemoryKb());
        log.info("=== [Scenario 1] Completed submission lifecycle test successfully ===");
    }

    @Test
    @Order(2)
    @DisplayName("Kịch bản 2: Kiểm thử tích hợp CodeHarnessService tự động bọc hàm main cho Python, Java và C++")
    void test2_CodeHarness_WrappingPythonJavaCpp() {
        log.info("=== [Scenario 2] Starting CodeHarnessService main-wrapper test (Python, Java, C++) ===");
        String pyUserCode = "class Solution:\n    def twoSum(self, nums: list[int], target: int) -> list[int]:\n        return [0, 1]";
        String wrappedPy = codeHarnessService.prepareSourceCode("two-sum", 71, pyUserCode);
        assertTrue(wrappedPy.contains("sys.stdin.read()"), "Python harness phải bọc lệnh đọc stdin");
        assertTrue(wrappedPy.contains("Solution()"), "Python harness phải khởi tạo đối tượng Solution()");
        assertTrue(wrappedPy.contains("class ListNode"), "Python harness phải định nghĩa sẵn cấu trúc dữ liệu phụ trợ");
        log.info("[HARNESS - PYTHON] Wrapped source code successfully ({} -> {} chars, contains sys.stdin.read / Solution / ListNode)",
                pyUserCode.length(), wrappedPy.length());

        String javaUserCode = "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        return new int[]{0, 1};\n    }\n}";
        String wrappedJava = codeHarnessService.prepareSourceCode("two-sum", 62, javaUserCode);
        assertTrue(wrappedJava.contains("public static void main(String[] args)"), "Java harness phải sinh hàm main()");
        assertTrue(wrappedJava.contains("Solution"), "Java harness phải tham chiếu lớp Solution của học viên");
        log.info("[HARNESS - JAVA] Wrapped source code successfully ({} -> {} chars, contains public static void main / Solution)",
                javaUserCode.length(), wrappedJava.length());

        String cppUserCode = "class Solution {\npublic:\n    vector<int> twoSum(vector<int>& nums, int target) {\n        return {0, 1};\n    }\n};";
        String wrappedCpp = codeHarnessService.prepareSourceCode("two-sum", 54, cppUserCode);
        assertTrue(wrappedCpp.contains("int main()"), "C++ harness phải sinh hàm int main()");
        assertTrue(wrappedCpp.contains("twoSum"), "C++ harness phải gọi hàm twoSum");
        log.info("[HARNESS - C++] Wrapped source code successfully ({} -> {} chars, contains int main() / twoSum)",
                cppUserCode.length(), wrappedCpp.length());
        log.info("=== [Scenario 2] Completed CodeHarnessService test successfully ===");
    }

    @Test
    @Order(3)
    @DisplayName("Kịch bản 3: Kiểm thử tích hợp chống gian lận điểm XP (Anti-farm XP) và truy vấn danh sách bài đã giải")
    void test3_SolvedStatus_And_AntiDuplicateXpCheck() {
        log.info("=== [Scenario 3] Starting Anti-farm XP & solved problem slugs test ===");
        boolean solvedBefore = submissionDAO.existsByUserIdAndProblemIdAndStatus(
                testUser.getId(), testProblem.getId(), "ACCEPTED");
        assertFalse(solvedBefore, "Học viên chưa giải bài này nên phải trả về false");
        log.info("[CHECK 1] Before any submission -> solved (ACCEPTED) = {}", solvedBefore);

        submissionDAO.insert(Submission.builder()
                .user(testUser)
                .problem(testProblem)
                .sourceCode("return []")
                .languageId(71)
                .status("WRONG_ANSWER")
                .build());
        boolean solvedAfterWrong = submissionDAO.existsByUserIdAndProblemIdAndStatus(
                testUser.getId(), testProblem.getId(), "ACCEPTED");
        assertFalse(solvedAfterWrong);
        log.info("[CHECK 2] After WRONG_ANSWER submission -> solved (ACCEPTED) = {}", solvedAfterWrong);

        submissionDAO.insert(Submission.builder()
                .user(testUser)
                .problem(testProblem)
                .sourceCode("return [0,1]")
                .languageId(71)
                .status("ACCEPTED")
                .totalRuntimeMs(18.0)
                .maxMemoryKb(12000)
                .build());

        boolean solvedAfterAccepted = submissionDAO.existsByUserIdAndProblemIdAndStatus(
                testUser.getId(), testProblem.getId(), "ACCEPTED");
        assertTrue(solvedAfterAccepted,
                "Sau khi có bài ACCEPTED, cờ đã giải phải là true để lần nộp sau không cộng trùng XP");
        log.info("[CHECK 3] After ACCEPTED submission -> solved (ACCEPTED) = {} (blocks duplicate XP on subsequent submissions)",
                solvedAfterAccepted);

        List<String> solvedSlugs = submissionDAO.findSolvedProblemSlugsByUserId(testUser.getId());
        assertTrue(solvedSlugs.contains(testProblem.getSlug()),
                "Danh sách slug bài đã giải phải chứa bài tập vừa ACCEPTED");
        log.info("[SOLVED SLUGS] Solved problem slugs for userId={} -> {}", testUser.getId(), solvedSlugs);
        log.info("=== [Scenario 3] Completed Anti-farm XP test successfully ===");
    }

    @Test
    @Order(4)
    @DisplayName("Kịch bản 4: Kiểm thử tích hợp Luồng Chấm bài sai ở Test Case ẩn và cơ chế che giấu Input/Output bí mật")
    void test4_GradingFlow_WrongAnswerOnHiddenCase_MasksSecretData() {
        log.info("=== [Scenario 4] Starting hidden test case grading & secret data masking test ===");
        ProblemRepository problemRepo = mock(ProblemRepository.class);
        TestCaseRepository testCaseRepo = mock(TestCaseRepository.class);
        SubmissionRepository submissionRepo = mock(SubmissionRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        Judge0Client judge0Client = mock(Judge0Client.class);
        SubmissionStreamManager streamManager = new SubmissionStreamManager();

        when(problemRepo.findBySlug(testProblem.getSlug())).thenReturn(Optional.of(testProblem));
        when(testCaseRepo.findByProblemId(testProblem.getId())).thenReturn(List.of(publicCase, hiddenCase));
        when(userRepo.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(submissionRepo.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            if (s.getId() == null) {
                return submissionDAO.insert(s);
            }
            return submissionDAO.updateStatusAndMetrics(s.getId(), s.getStatus(), s.getTotalRuntimeMs(), s.getMaxMemoryKb());
        });
        when(submissionRepo.findById(any(UUID.class))).thenAnswer(inv -> submissionDAO.findById(inv.getArgument(0)));

        Judge0Response res1 = new Judge0Response();
        res1.setStdout("[0, 1]\n");
        res1.setTime(0.015);
        res1.setMemory(11000);
        Judge0Response.Status st1 = new Judge0Response.Status();
        st1.setId(Judge0Status.ACCEPTED.getId());
        res1.setStatus(st1);

        Judge0Response res2 = new Judge0Response();
        res2.setStdout("[0, 0]\n");
        res2.setTime(0.020);
        res2.setMemory(11500);
        Judge0Response.Status st2 = new Judge0Response.Status();
        st2.setId(Judge0Status.ACCEPTED.getId());
        res2.setStatus(st2);

        when(judge0Client.executeBatch(any())).thenReturn(List.of(res1, res2));

        SubmissionServiceImpl submissionService = new SubmissionServiceImpl(
                problemRepo, testCaseRepo, submissionRepo, userRepo, judge0Client, streamManager, codeHarnessService);

        SubmitCodeRequest req = SubmitCodeRequest.builder()
                .problemSlug(testProblem.getSlug())
                .userId(testUser.getId())
                .languageId(71)
                .sourceCode("class Solution:\n    def twoSum(self, nums, target):\n        return [0, 1]")
                .build();

        log.info("[SUBMIT] Evaluating submission slug='{}', languageId={} (2 test cases: 1 public + 1 hidden)",
                req.getProblemSlug(), req.getLanguageId());
        SubmissionResponse response = submissionService.submitAndEvaluate(req);

        assertEquals("WRONG_ANSWER", response.getStatus(), "Sai ở test case ẩn phải trả về trạng thái WRONG_ANSWER");
        assertEquals(1, response.getPassedTestCases(), "Chỉ vượt qua 1/2 test cases");
        assertEquals(0, response.getXpEarned(), "Giải sai không được cộng điểm XP");
        log.info("[GRADING RESULT] status='{}', passed={}/{}, xpEarned={}",
                response.getStatus(), response.getPassedTestCases(), response.getTestCaseResults().size(), response.getXpEarned());

        TestCaseResult pubResult = response.getTestCaseResults().get(0);
        assertFalse(pubResult.isHidden());
        assertNotNull(pubResult.getInputData(), "Test case công khai được phép hiển thị input");
        log.info("[PUBLIC CASE] isHidden={}, status='{}', inputData='{}'",
                pubResult.isHidden(), pubResult.getStatus(),
                pubResult.getInputData().replace("\n", "\\n"));

        TestCaseResult hidResult = response.getTestCaseResults().get(1);
        assertTrue(hidResult.isHidden());
        assertNull(hidResult.getInputData(), "Test case ẩn phải che giấu inputData (null)");
        assertNull(hidResult.getExpectedOutput(), "Test case ẩn phải che giấu expectedOutput (null)");
        assertNull(hidResult.getActualOutput(), "Test case ẩn phải che giấu actualOutput (null)");
        log.info("[HIDDEN CASE] isHidden={}, status='{}', inputData={}, expectedOutput={}, actualOutput={} (masked)",
                hidResult.isHidden(), hidResult.getStatus(),
                hidResult.getInputData(), hidResult.getExpectedOutput(), hidResult.getActualOutput());
        log.info("=== [Scenario 4] Completed hidden test case masking test successfully ===");
    }

    @Test
    @Order(5)
    @DisplayName("Kịch bản 5: Kiểm thử tích hợp Truy vấn lịch sử nộp bài của học viên và xóa bài nộp")
    void test5_FindSubmissionHistoryByUserId_And_Delete() {
        log.info("=== [Scenario 5] Starting submission history query & delete test ===");
        Submission s1 = submissionDAO.insert(Submission.builder()
                .user(testUser)
                .problem(testProblem)
                .sourceCode("print(1)")
                .languageId(71)
                .status("WRONG_ANSWER")
                .build());

        Submission s2 = submissionDAO.insert(Submission.builder()
                .user(testUser)
                .problem(testProblem)
                .sourceCode("print(2)")
                .languageId(71)
                .status("ACCEPTED")
                .build());
        log.info("[INSERT] Created 2 sample submissions: s1(id={}, status='{}'), s2(id={}, status='{}')",
                s1.getId(), s1.getStatus(), s2.getId(), s2.getStatus());

        List<Submission> history = submissionDAO.findByUserId(testUser.getId());
        assertTrue(history.size() >= 2, "Lịch sử nộp bài phải ghi nhận đủ các lần nộp");
        log.info("[HISTORY] Queried findByUserId({}) -> returned {} submissions", testUser.getId(), history.size());

        boolean del1 = submissionDAO.delete(s1.getId());
        boolean del2 = submissionDAO.delete(s2.getId());
        assertTrue(del1);
        assertTrue(del2);
        log.info("[DELETE] Deleted s1 (id={}) -> {}, deleted s2 (id={}) -> {}", s1.getId(), del1, s2.getId(), del2);
        log.info("=== [Scenario 5] Completed submission history & delete test successfully ===");
    }
}
