package com.dapcode.backend_core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dapcode.backend_core.common.ErrorCode;
import com.dapcode.backend_core.dto.admin.CreateProblemRequest;
import com.dapcode.backend_core.dto.admin.UpdateProblemRequest;
import com.dapcode.backend_core.dto.problem.ProblemDetailResponse;
import com.dapcode.backend_core.entity.Problem;
import com.dapcode.backend_core.exception.AppException;
import com.dapcode.backend_core.repository.ProblemRepository;
import com.dapcode.backend_core.repository.TestCaseRepository;
import com.dapcode.backend_core.service.problem.ProblemServiceImpl;

@ExtendWith(MockitoExtension.class)
public class ProblemServiceTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @InjectMocks
    private ProblemServiceImpl problemService;

    private Problem sampleProblem;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        sampleProblem = Problem.builder()
                .id(sampleId)
                .slug("two-sum")
                .title("Two Sum")
                .difficulty("EASY")
                .category("Arrays & Hashing")
                .descriptionMarkdown("Tìm hai số có tổng bằng target")
                .timeLimitMs(1000)
                .memoryLimitKb(131072)
                .isPublished(true)
                .build();
    }

    @Test
    void testCreateProblem_Success() {
        CreateProblemRequest req = CreateProblemRequest.builder()
                .title("Two Sum")
                .slug("two-sum")
                .difficulty("EASY")
                .category("Arrays & Hashing")
                .descriptionMarkdown("Tìm hai số có tổng bằng target")
                .timeLimitMs(1000)
                .memoryLimitKb(131072)
                .isPublished(true)
                .build();

        when(problemRepository.existsBySlug("two-sum")).thenReturn(false);
        when(problemRepository.save(any(Problem.class))).thenReturn(sampleProblem);
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(sampleProblem));
        when(testCaseRepository.findByProblemIdAndIsHiddenFalse(sampleId)).thenReturn(Collections.emptyList());

        ProblemDetailResponse res = problemService.createProblem(req);

        assertNotNull(res);
        assertEquals("Two Sum", res.getTitle());
        assertEquals("EASY", res.getDifficulty());
        verify(problemRepository).save(any(Problem.class));
    }

    @Test
    void testUpdateProblem_Success() {
        UpdateProblemRequest req = UpdateProblemRequest.builder()
                .title("Two Sum Updated")
                .difficulty("MEDIUM")
                .category("Arrays & Hashing")
                .descriptionMarkdown("Mô tả mới")
                .timeLimitMs(2000)
                .memoryLimitKb(262144)
                .isPublished(true)
                .build();

        when(problemRepository.findById(sampleId)).thenReturn(Optional.of(sampleProblem));
        when(problemRepository.save(any(Problem.class))).thenReturn(sampleProblem);
        when(problemRepository.findBySlug(sampleProblem.getSlug())).thenReturn(Optional.of(sampleProblem));
        when(testCaseRepository.findByProblemIdAndIsHiddenFalse(sampleId)).thenReturn(Collections.emptyList());

        ProblemDetailResponse res = problemService.updateProblem(sampleId, req);

        assertNotNull(res);
        verify(problemRepository).save(sampleProblem);
    }

    @Test
    void testDeleteProblem_Success() {
        when(problemRepository.findById(sampleId)).thenReturn(Optional.of(sampleProblem));

        problemService.deleteProblem(sampleId);

        verify(problemRepository).delete(sampleProblem);
    }

    @Test
    void testGetProblemBySlug_NotFound() {
        when(problemRepository.findBySlug("unknown-slug")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> problemService.getProblemBySlug("unknown-slug"));
        assertEquals(ErrorCode.PROBLEM_NOT_FOUND, ex.getErrorCode());
    }
}
