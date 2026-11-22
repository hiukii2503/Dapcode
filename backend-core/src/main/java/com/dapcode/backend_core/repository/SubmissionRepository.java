package com.dapcode.backend_core.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dapcode.backend_core.dto.admin.DailySubmissionProjection;
import com.dapcode.backend_core.entity.Submission;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    List<Submission> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Submission> findByUserIdAndProblemIdOrderByCreatedAtDesc(UUID userId, UUID problemId);

    long countByStatus(String status);

    @EntityGraph(attributePaths = {"user", "problem"})
    List<Submission> findTop5ByOrderByCreatedAtDesc();

    @Query(value = """
            SELECT TO_CHAR(created_at, 'YYYY-MM-DD') AS dateStr, COUNT(*) AS count
            FROM submissions
            WHERE created_at >= :startDate
            GROUP BY TO_CHAR(created_at, 'YYYY-MM-DD')
            """, nativeQuery = true)
    List<DailySubmissionProjection> countSubmissionsGroupedByDate(@Param("startDate") Instant startDate);

    @Query("SELECT DISTINCT s.problem.slug FROM Submission s WHERE s.user.id = :userId AND s.status = 'ACCEPTED'")
    List<String> findSolvedProblemSlugsByUserId(@Param("userId") UUID userId);

    boolean existsByUserIdAndProblemIdAndStatus(UUID userId, UUID problemId, String status);

    Optional<Submission> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);
}
