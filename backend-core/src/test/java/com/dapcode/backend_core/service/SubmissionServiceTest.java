package com.dapcode.backend_core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dapcode.backend_core.dto.submission.SubmissionDetailResponse;
import com.dapcode.backend_core.dto.submission.SubmissionResponse;
import com.dapcode.backend_core.dto.submission.SubmitCodeRequest;
import com.dapcode.backend_core.entity.Problem;
import com.dapcode.backend_core.entity.Submission;
import com.dapcode.backend_core.entity.TestCase;
import com.dapcode.backend_core.entity.Users;
import com.dapcode.backend_core.exception.AppException;
import com.dapcode.backend_core.judge0.Judge0Client;
import com.dapcode.backend_core.judge0.Judge0Response;
import com.dapcode.backend_core.judge0.Judge0Status;
import com.dapcode.backend_core.repository.ProblemRepository;
import com.dapcode.backend_core.repository.SubmissionRepository;
import com.dapcode.backend_core.repository.TestCaseRepository;
import com.dapcode.backend_core.repository.UserRepository;
import com.dapcode.backend_core.service.submission.CodeHarnessService;
import com.dapcode.backend_core.service.submission.SubmissionServiceImpl;
import com.dapcode.backend_core.service.submission.SubmissionStreamManager;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Judge0Client judge0Client;

    @Mock
    private SubmissionStreamManager streamManager;

    @Mock
    private CodeHarnessService codeHarnessService;

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    private Problem sampleProblem;
    private Users sampleUser;
    private TestCase sampleTestCase;

    @BeforeEach
    void setUp() {
        sampleProblem = Problem.builder()
                .id(UUID.randomUUID())
                .slug("two-sum")
                .title("Two Sum")
                .difficulty("Easy")
                .timeLimitMs(2000)
                .memoryLimitKb(262144)
                .build();

        sampleUser = Users.builder()
                .id(UUID.randomUUID())
                .username("student01")
                .email("student01@dapcode.io")
                .role("ROLE_USER")
                .xp(100)
                .streakCount(2)
                .isActive(true)
                .build();

        sampleTestCase = TestCase.builder()
                .id(UUID.randomUUID())
                .problem(sampleProblem)
                .inputData("[2,7,11,15]\n9")
                .expectedOutput("[0,1]")
                .isHidden(false)
                .build();
    }

    @Test
    @DisplayName("Nộp bài thành công đạt ACCEPTED và cộng điểm XP cho học viên")
    void submitAndEvaluate_Accepted_Success() {
        SubmitCodeRequest req = SubmitCodeRequest.builder()
                .problemSlug("two-sum")
                .userId(sampleUser.getId())
                .languageId(71)
                .sourceCode("class Solution:\n    def twoSum(self, nums, target):\n        return [0,1]")
                .build();

        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(sampleProblem));
        when(testCaseRepository.findByProblemId(sampleProblem.getId())).thenReturn(List.of(sampleTestCase));
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByIdForUpdate(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        final Submission[] savedHolder = new Submission[1];
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            savedHolder[0] = s;
            return s;
        });
        when(submissionRepository.findById(any(UUID.class))).thenAnswer(inv -> Optional.ofNullable(savedHolder[0]));
        when(codeHarnessService.prepareSourceCode(any(), anyInt(), any())).thenReturn(req.getSourceCode());

        Judge0Response jRes = new Judge0Response();
        jRes.setStdout("[0, 1]\n");
        jRes.setTime(0.025);
        jRes.setMemory(10240);
        Judge0Response.Status st = new Judge0Response.Status();
        st.setId(Judge0Status.ACCEPTED.getId());
        st.setDescription("Accepted");
        jRes.setStatus(st);

        when(judge0Client.executeBatch(any())).thenReturn(List.of(jRes));
        when(submissionRepository.existsByUserIdAndProblemIdAndStatus(sampleUser.getId(), sampleProblem.getId(), "ACCEPTED"))
                .thenReturn(false);
        when(submissionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(sampleUser.getId(), "ACCEPTED"))
                .thenReturn(Optional.empty());

        SubmissionResponse response = submissionService.submitAndEvaluate(req);

        assertNotNull(response);
        assertEquals("ACCEPTED", response.getStatus());
        assertEquals(1, response.getPassedTestCases());
        assertEquals(20, response.getXpEarned());
        assertEquals(120, sampleUser.getXp());
        verify(submissionRepository, times(2)).save(any(Submission.class));
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("Nộp bài khi bài tập chưa có test case sẽ ném ngoại lệ TEST_CASE_NOT_FOUND")
    void submitAndEvaluate_NoTestCases_ThrowsException() {
        SubmitCodeRequest req = SubmitCodeRequest.builder()
                .problemSlug("two-sum")
                .languageId(71)
                .sourceCode("print(1)")
                .build();

        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(sampleProblem));
        when(testCaseRepository.findByProblemId(sampleProblem.getId())).thenReturn(List.of());

        assertThrows(AppException.class, () -> submissionService.submitAndEvaluate(req));
    }

    @Test
    @DisplayName("Xem chi tiết bài nộp của chính mình thành công")
    void getSubmissionById_Owner_Success() {
        UUID subId = UUID.randomUUID();
        Submission sub = Submission.builder()
                .id(subId)
                .user(sampleUser)
                .problem(sampleProblem)
                .sourceCode("print('ok')")
                .languageId(71)
                .status("ACCEPTED")
                .totalRuntimeMs(15.0)
                .maxMemoryKb(8192)
                .build();

        when(submissionRepository.findById(subId)).thenReturn(Optional.of(sub));

        SubmissionDetailResponse detail = submissionService.getSubmissionById(subId, sampleUser);

        assertNotNull(detail);
        assertEquals(subId, detail.getId());
        assertEquals("ACCEPTED", detail.getStatus());
        assertEquals("two-sum", detail.getProblemSlug());
    }
}
