package com.dapcode.backend_core.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.dapcode.backend_core.entity.Problem;
import com.dapcode.backend_core.entity.Submission;
import com.dapcode.backend_core.entity.Users;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class SubmissionDAOImpl implements SubmissionDAO {

    private Submission mapResultSetToSubmission(ResultSet rs) throws SQLException {
        UUID userId = (UUID) rs.getObject("user_id");
        UUID problemId = (UUID) rs.getObject("problem_id");

        Double runtime = rs.getDouble("total_runtime_ms");
        if (rs.wasNull()) {
            runtime = null;
        }
        Integer memory = rs.getInt("max_memory_kb");
        if (rs.wasNull()) {
            memory = null;
        }

        return Submission.builder()
                .id((UUID) rs.getObject("id"))
                .user(userId != null ? Users.builder().id(userId).build() : null)
                .problem(problemId != null ? Problem.builder().id(problemId).build() : null)
                .sourceCode(rs.getString("source_code"))
                .languageId(rs.getInt("language_id"))
                .status(rs.getString("status"))
                .totalRuntimeMs(runtime)
                .maxMemoryKb(memory)
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null)
                .build();
    }

    @Override
    public Optional<Submission> findById(UUID id) {
        String sql = "SELECT * FROM submissions WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSubmission(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi khi findById submission qua JDBC: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Submission> findByUserId(UUID userId) {
        String sql = "SELECT * FROM submissions WHERE user_id = ? ORDER BY created_at DESC";
        List<Submission> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSubmission(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi khi findByUserId submissions qua JDBC: {}", e.getMessage());
        }
        return list;
    }

    @Override
    public List<String> findSolvedProblemSlugsByUserId(UUID userId) {
        String sql = """
                SELECT DISTINCT p.slug
                FROM submissions s
                JOIN problems p ON s.problem_id = p.id
                WHERE s.user_id = ? AND s.status = 'ACCEPTED'
                """;
        List<String> slugs = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    slugs.add(rs.getString("slug"));
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi khi findSolvedProblemSlugsByUserId qua JDBC: {}", e.getMessage());
        }
        return slugs;
    }

    @Override
    public boolean existsByUserIdAndProblemIdAndStatus(UUID userId, UUID problemId, String status) {
        String sql = "SELECT 1 FROM submissions WHERE user_id = ? AND problem_id = ? AND status = ? LIMIT 1";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, userId);
            ps.setObject(2, problemId);
            ps.setString(3, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Lỗi khi existsByUserIdAndProblemIdAndStatus qua JDBC: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public Submission insert(Submission submission) {
        String sql = """
                INSERT INTO submissions (id, user_id, problem_id, source_code, language_id, status, total_runtime_ms, max_memory_kb, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        UUID newId = submission.getId() != null ? submission.getId() : UUID.randomUUID();
        submission.setId(newId);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, newId);
            ps.setObject(2, submission.getUser() != null ? submission.getUser().getId() : null);
            ps.setObject(3, submission.getProblem() != null ? submission.getProblem().getId() : null);
            ps.setString(4, submission.getSourceCode());
            ps.setInt(5, submission.getLanguageId());
            ps.setString(6, submission.getStatus() != null ? submission.getStatus() : "PENDING");

            if (submission.getTotalRuntimeMs() != null) {
                ps.setDouble(7, submission.getTotalRuntimeMs());
            } else {
                ps.setNull(7, Types.DOUBLE);
            }

            if (submission.getMaxMemoryKb() != null) {
                ps.setInt(8, submission.getMaxMemoryKb());
            } else {
                ps.setNull(8, Types.INTEGER);
            }

            ps.setTimestamp(9, now);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                submission.setCreatedAt(now.toInstant());
                return submission;
            }
        } catch (SQLException e) {
            log.error("Lỗi khi insert submission qua JDBC: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Submission updateStatusAndMetrics(UUID id, String status, Double totalRuntimeMs, Integer maxMemoryKb) {
        String sql = """
                UPDATE submissions
                SET status = ?, total_runtime_ms = ?, max_memory_kb = ?
                WHERE id = ?
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            if (totalRuntimeMs != null) {
                ps.setDouble(2, totalRuntimeMs);
            } else {
                ps.setNull(2, Types.DOUBLE);
            }
            if (maxMemoryKb != null) {
                ps.setInt(3, maxMemoryKb);
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setObject(4, id);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                return findById(id).orElse(null);
            }
        } catch (SQLException e) {
            log.error("Lỗi khi updateStatusAndMetrics submission qua JDBC: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean delete(UUID id) {
        String sql = "DELETE FROM submissions WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Lỗi khi delete submission qua JDBC: {}", e.getMessage());
        }
        return false;
    }
}
