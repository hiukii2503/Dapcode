package com.dapcode.backend_core.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dapcode.backend_core.entity.Submission;

public interface SubmissionDAO {

    Optional<Submission> findById(UUID id);

    List<Submission> findByUserId(UUID userId);

    List<String> findSolvedProblemSlugsByUserId(UUID userId);

    boolean existsByUserIdAndProblemIdAndStatus(UUID userId, UUID problemId, String status);

    Submission insert(Submission submission);

    Submission updateStatusAndMetrics(UUID id, String status, Double totalRuntimeMs, Integer maxMemoryKb);

    boolean delete(UUID id);
}
