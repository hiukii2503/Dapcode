package com.dapcode.backend_core.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.dapcode.backend_core.entity.Problem;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class ProblemDAOImpl implements ProblemDAO {

    private Problem mapResultSetToProblem(ResultSet rs) throws SQLException {
        return Problem.builder()
                .id((UUID) rs.getObject("id"))
                .slug(rs.getString("slug"))
                .title(rs.getString("title"))
                .difficulty(rs.getString("difficulty"))
                .category(rs.getString("category"))
                .descriptionMarkdown(rs.getString("description_markdown"))
                .timeLimitMs(rs.getInt("time_limit_ms"))
                .memoryLimitKb(rs.getInt("memory_limit_kb"))
                .starterCode(rs.getString("starter_code"))
                .isPublished(rs.getBoolean("is_published"))
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : null)
                .build();
    }

    @Override
    public List<Problem> findAll() {
        List<Problem> list = new ArrayList<>();
        String sql = "SELECT * FROM problems ORDER BY created_at DESC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToProblem(rs));
            }
        } catch (SQLException e) {
            log.error("Lỗi khi findAll problems: {}", e.getMessage());
        }
        return list;
    }

    @Override
    public Optional<Problem> findById(UUID id) {
        String sql = "SELECT * FROM problems WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProblem(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi khi findById problem: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Problem> findBySlug(String slug) {
        String sql = "SELECT * FROM problems WHERE slug = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProblem(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi khi findBySlug problem: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public boolean existsBySlug(String slug) {
        String sql = "SELECT 1 FROM problems WHERE slug = ? LIMIT 1";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Lỗi khi existsBySlug problem: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public Problem insert(Problem problem) {
        String sql = """
                INSERT INTO problems (id, slug, title, difficulty, category, description_markdown, time_limit_ms, memory_limit_kb, starter_code, is_published, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        UUID newId = problem.getId() != null ? problem.getId() : UUID.randomUUID();
        problem.setId(newId);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, newId);
            ps.setString(2, problem.getSlug());
            ps.setString(3, problem.getTitle());
            ps.setString(4, problem.getDifficulty());
            ps.setString(5, problem.getCategory());
            ps.setString(6, problem.getDescriptionMarkdown());
            ps.setInt(7, problem.getTimeLimitMs());
            ps.setInt(8, problem.getMemoryLimitKb());
            ps.setString(9, problem.getStarterCode());
            ps.setBoolean(10, problem.isPublished());
            ps.setTimestamp(11, now);
            ps.setTimestamp(12, now);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                problem.setCreatedAt(now.toInstant());
                problem.setUpdatedAt(now.toInstant());
                return problem;
            }
        } catch (SQLException e) {
            log.error("Lỗi khi insert problem: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Problem update(Problem problem) {
        String sql = """
                UPDATE problems
                SET title = ?, difficulty = ?, category = ?, description_markdown = ?, time_limit_ms = ?, memory_limit_kb = ?, starter_code = ?, is_published = ?, updated_at = ?
                WHERE id = ?
                """;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, problem.getTitle());
            ps.setString(2, problem.getDifficulty());
            ps.setString(3, problem.getCategory());
            ps.setString(4, problem.getDescriptionMarkdown());
            ps.setInt(5, problem.getTimeLimitMs());
            ps.setInt(6, problem.getMemoryLimitKb());
            ps.setString(7, problem.getStarterCode());
            ps.setBoolean(8, problem.isPublished());
            ps.setTimestamp(9, now);
            ps.setObject(10, problem.getId());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                problem.setUpdatedAt(now.toInstant());
                return problem;
            }
        } catch (SQLException e) {
            log.error("Lỗi khi update problem: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean delete(UUID id) {
        String sql = "DELETE FROM problems WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Lỗi khi delete problem: {}", e.getMessage());
        }
        return false;
    }
}
