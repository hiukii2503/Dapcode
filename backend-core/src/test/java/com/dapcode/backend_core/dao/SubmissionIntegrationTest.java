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

/**
 * Kiểm thử tích hợp (Integration Tests) cho Luồng Nộp bài (Submission) và Luồng Chấm bài (Grading).
 * Kết hợp truy vấn trực tiếp trên PostgreSQL qua HikariCP (SubmissionDAO, ProblemDAO, UserDAO, TestCaseDAO)
 * cùng với bộ bọc code tự động CodeHarnessServiceImpl và logic chấm điểm SubmissionServiceImpl.
 */
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
    }

    @AfterEach
    void tearDown() {
        if (testProblem != null && testProblem.getId() != null) {
            problemDAO.delete(testProblem.getId());
        }
        if (testUser != null && testUser.getId() != null) {
            try (var conn = DBContext.getConnection();
                 var ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                ps.setObject(1, testUser.getId());
                ps.executeUpdate();
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("Kịch bản 1: Luồng nộp bài từ trạng thái PENDING sang ACCEPTED và cập nhật Runtime/Memory trên DB thật")
    void test1_SubmissionLifecycle_PendingToAccepted() {
        // 1. Khi học viên bấm Submit -> Lưu trạng thái PENDING
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

        // 2. Sau khi Judge0 chấm xong -> Cập nhật trạng thái ACCEPTED cùng thời gian chạy và bộ nhớ
        Submission updated = submissionDAO.updateStatusAndMetrics(saved.getId(), "ACCEPTED", 24.5, 14200);
        assertNotNull(updated);
        assertEquals("ACCEPTED", updated.getStatus());
        assertEquals(24.5, updated.getTotalRuntimeMs());
        assertEquals(14200, updated.getMaxMemoryKb());

        // 3. Kiểm tra đọc lại từ Database
        Optional<Submission> fromDb = submissionDAO.findById(saved.getId());
        assertTrue(fromDb.isPresent());
        assertEquals("ACCEPTED", fromDb.get().getStatus());
    }

    @Test
    @Order(2)
    @DisplayName("Kịch bản 2: Kiểm thử tích hợp CodeHarnessService tự động bọc hàm main cho Python, Java và C++")
    void test2_CodeHarness_WrappingPythonJavaCpp() {
        String pyUserCode = "class Solution:\n    def twoSum(self, nums: list[int], target: int) -> list[int]:\n        return [0, 1]";
        String wrappedPy = codeHarnessService.prepareSourceCode("two-sum", 71, pyUserCode);
        assertTrue(wrappedPy.contains("sys.stdin.read()"), "Python harness phải bọc lệnh đọc stdin");
        assertTrue(wrappedPy.contains("Solution()"), "Python harness phải khởi tạo đối tượng Solution()");
        assertTrue(wrappedPy.contains("class ListNode"), "Python harness phải định nghĩa sẵn cấu trúc dữ liệu phụ trợ");

        String javaUserCode = "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        return new int[]{0, 1};\n    }\n}";
        String wrappedJava = codeHarnessService.prepareSourceCode("two-sum", 62, javaUserCode);
        assertTrue(wrappedJava.contains("public static void main(String[] args)"), "Java harness phải sinh hàm main()");
        assertTrue(wrappedJava.contains("Solution"), "Java harness phải tham chiếu lớp Solution của học viên");

        String cppUserCode = "class Solution {\npublic:\n    vector<int> twoSum(vector<int>& nums, int target) {\n        return {0, 1};\n    }\n};";
        String wrappedCpp = codeHarnessService.prepareSourceCode("two-sum", 54, cppUserCode);
        assertTrue(wrappedCpp.contains("int main()"), "C++ harness phải sinh hàm int main()");
        assertTrue(wrappedCpp.contains("twoSum"), "C++ harness phải gọi hàm twoSum");
    }

    @Test
    @Order(3)
    @DisplayName("Kịch bản 3: Kiểm thử tích hợp chống gian lận điểm XP (Anti-farm XP) và truy vấn danh sách bài đã giải")
    void test3_SolvedStatus_And_AntiDuplicateXpCheck() {
        // Trước khi có bài nộp ACCEPTED -> existsByUserIdAndProblemIdAndStatus phải trả về false
        boolean solvedBefore = submissionDAO.existsByUserIdAndProblemIdAndStatus(
                testUser.getId(), testProblem.getId(), "ACCEPTED");
        assertFalse(solvedBefore, "Học viên chưa giải bài này nên phải trả về false");

        // Nộp 1 bài bị WRONG_ANSWER -> vẫn chưa được tính là đã giải
        submissionDAO.insert(Submission.builder()
                .user(testUser)
                .problem(testProblem)
                .sourceCode("return []")
                .languageId(71)
                .status("WRONG_ANSWER")
                .build());
        assertFalse(submissionDAO.existsByUserIdAndProblemIdAndStatus(
                testUser.getId(), testProblem.getId(), "ACCEPTED"));

        // Nộp bài đạt ACCEPTED
        submissionDAO.insert(Submission.builder()
                .user(testUser)
                .problem(testProblem)
                .sourceCode("return [0,1]")
                .languageId(71)
                .status("ACCEPTED")
                .totalRuntimeMs(18.0)
                .maxMemoryKb(12000)
                .build());

        assertTrue(submissionDAO.existsByUserIdAndProblemIdAndStatus(
                testUser.getId(), testProblem.getId(), "ACCEPTED"),
                "Sau khi có bài ACCEPTED, cờ đã giải phải là true để lần nộp sau không cộng trùng XP");

        List<String> solvedSlugs = submissionDAO.findSolvedProblemSlugsByUserId(testUser.getId());
        assertTrue(solvedSlugs.contains(testProblem.getSlug()),
                "Danh sách slug bài đã giải phải chứa bài tập vừa ACCEPTED");
    }

    @Test
    @Order(4)
    @DisplayName("Kịch bản 4: Kiểm thử tích hợp Luồng Chấm bài sai ở Test Case ẩn và cơ chế che giấu Input/Output bí mật")
    void test4_GradingFlow_WrongAnswerOnHiddenCase_MasksSecretData() {
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

        // Test case 1 (Public) trả kết quả ĐÚNG "[0, 1]", Test case 2 (Hidden) trả kết quả SAI "[0, 0]"
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

        SubmissionResponse response = submissionService.submitAndEvaluate(req);

        assertEquals("WRONG_ANSWER", response.getStatus(), "Sai ở test case ẩn phải trả về trạng thái WRONG_ANSWER");
        assertEquals(1, response.getPassedTestCases(), "Chỉ vượt qua 1/2 test cases");
        assertEquals(0, response.getXpEarned(), "Giải sai không được cộng điểm XP");

        // Kiểm tra bảo mật: Test case công khai phải hiển thị input/output, Test case ẩn phải bị gán null
        TestCaseResult pubResult = response.getTestCaseResults().get(0);
        assertFalse(pubResult.isHidden());
        assertNotNull(pubResult.getInputData(), "Test case công khai được phép hiển thị input");

        TestCaseResult hidResult = response.getTestCaseResults().get(1);
        assertTrue(hidResult.isHidden());
        assertNull(hidResult.getInputData(), "Test case ẩn phải che giấu inputData (null)");
        assertNull(hidResult.getExpectedOutput(), "Test case ẩn phải che giấu expectedOutput (null)");
        assertNull(hidResult.getActualOutput(), "Test case ẩn phải che giấu actualOutput (null)");
    }

    @Test
    @Order(5)
    @DisplayName("Kịch bản 5: Kiểm thử tích hợp Truy vấn lịch sử nộp bài của học viên và xóa bài nộp")
    void test5_FindSubmissionHistoryByUserId_And_Delete() {
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

        List<Submission> history = submissionDAO.findByUserId(testUser.getId());
        assertTrue(history.size() >= 2, "Lịch sử nộp bài phải ghi nhận đủ các lần nộp");

        assertTrue(submissionDAO.delete(s1.getId()));
        assertTrue(submissionDAO.delete(s2.getId()));
    }
}
