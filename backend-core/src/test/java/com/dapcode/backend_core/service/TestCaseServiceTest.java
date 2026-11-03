package com.dapcode.backend_core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.dapcode.backend_core.common.ErrorCode;
import com.dapcode.backend_core.dto.admin.TestCaseRequest;
import com.dapcode.backend_core.dto.admin.TestCaseResponse;
import com.dapcode.backend_core.entity.Problem;
import com.dapcode.backend_core.entity.TestCase;
import com.dapcode.backend_core.exception.AppException;
import com.dapcode.backend_core.repository.ProblemRepository;
import com.dapcode.backend_core.repository.TestCaseRepository;
import com.dapcode.backend_core.service.testcase.TestCaseServiceImpl;

@ExtendWith(MockitoExtension.class)
public class TestCaseServiceTest {

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private ProblemRepository problemRepository;

    @InjectMocks
    private TestCaseServiceImpl testCaseService;

    private Problem sampleProblem;
    private UUID problemId;
    private TestCase sampleTestCase;
    private UUID testCaseId;

    @BeforeEach
    void setUp() {
        problemId = UUID.randomUUID();
        sampleProblem = Problem.builder()
                .id(problemId)
                .slug("two-sum")
                .title("Two Sum")
                .difficulty("EASY")
                .build();

        testCaseId = UUID.randomUUID();
        sampleTestCase = TestCase.builder()
                .id(testCaseId)
                .problem(sampleProblem)
                .inputData("[2,7,11,15]\n9")
                .expectedOutput("[0,1]")
                .isHidden(false)
                .build();
    }

    @Test
    void testCreateTestCase_Success() {
        TestCaseRequest req = TestCaseRequest.builder()
                .inputData("[3,2,4]\n6")
                .expectedOutput("[1,2]")
                .isHidden(true)
                .build();

        when(problemRepository.findById(problemId)).thenReturn(Optional.of(sampleProblem));
        when(testCaseRepository.save(any(TestCase.class))).thenAnswer(invocation -> {
            TestCase tc = invocation.getArgument(0);
            tc.setId(UUID.randomUUID());
            return tc;
        });

        TestCaseResponse res = testCaseService.createTestCase(problemId, req);

        assertNotNull(res);
        assertEquals("[3,2,4]\n6", res.getInputData());
        assertEquals("[1,2]", res.getExpectedOutput());
        assertEquals(true, res.isHidden());
        verify(testCaseRepository).save(any(TestCase.class));
    }

    @Test
    void testGetTestCasesByProblemId_Success() {
        when(problemRepository.existsById(problemId)).thenReturn(true);
        when(testCaseRepository.findByProblemId(problemId)).thenReturn(List.of(sampleTestCase));

        List<TestCaseResponse> result = testCaseService.getTestCasesByProblemId(problemId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("[2,7,11,15]\n9", result.get(0).getInputData());
        assertEquals("[0,1]", result.get(0).getExpectedOutput());
    }

    @Test
    void testDeleteTestCase_Success() {
        when(testCaseRepository.findById(testCaseId)).thenReturn(Optional.of(sampleTestCase));

        testCaseService.deleteTestCase(testCaseId);

        verify(testCaseRepository).delete(sampleTestCase);
    }

    @Test
    void testCreateTestCase_ProblemNotFound() {
        UUID unknownProblemId = UUID.randomUUID();
        TestCaseRequest req = TestCaseRequest.builder()
                .inputData("[1,2]\n3")
                .expectedOutput("[0,1]")
                .build();

        when(problemRepository.findById(unknownProblemId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> testCaseService.createTestCase(unknownProblemId, req));
        assertEquals(ErrorCode.PROBLEM_NOT_FOUND, ex.getErrorCode());
    }
}

