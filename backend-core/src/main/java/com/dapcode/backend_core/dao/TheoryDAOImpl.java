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

import com.dapcode.backend_core.entity.Theories;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class TheoryDAOImpl implements TheoryDAO {

    private Theories mapResultSetToTheory(ResultSet rs) throws SQLException {
        return Theories.builder()
                .id((UUID) rs.getObject("id"))
                .title(rs.getString("title"))
                .slug(rs.getString("slug"))
                .category(rs.getString("category"))
                .summary(rs.getString("summary"))
                .contentMarkdown(rs.getString("content_markdown"))
                .orderIndex(rs.getInt("order_index"))
                .isPublished(rs.getBoolean("is_published"))
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : null)
                .build();
    }

    @Override
    public List<Theories> findAll() {
        List<Theories> list = new ArrayList<>();
        String sql = "SELECT * FROM theories ORDER BY category ASC, order_index ASC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToTheory(rs));
            }
        } catch (SQLException e) {
            log.error("Lỗi khi findAll theories qua JDBC: {}", e.getMessage());
        }
        return list;
    }

    @Override
    public List<Theories> findByCategory(String category) {
        List<Theories> list = new ArrayList<>();
        String sql = "SELECT * FROM theories WHERE category = ? ORDER BY order_index ASC";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTheory(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi khi findByCategory theories qua JDBC: {}", e.getMessage());
        }
        return list;
    }

    @Override
    public Optional<Theories> findById(UUID id) {
        String sql = "SELECT * FROM theories WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTheory(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi khi findById theories qua JDBC: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Theories> findBySlug(String slug) {
        String sql = "SELECT * FROM theories WHERE slug = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTheory(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi khi findBySlug theories qua JDBC: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public boolean existsBySlug(String slug) {
        String sql = "SELECT 1 FROM theories WHERE slug = ? LIMIT 1";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Lỗi khi check existsBySlug theories qua JDBC: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public Theories insert(Theories theory) {
        String sql = """
                INSERT INTO theories (id, title, slug, category, summary, content_markdown, order_index, is_published, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        UUID newId = theory.getId() != null ? theory.getId() : UUID.randomUUID();
        theory.setId(newId);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, newId);
            ps.setString(2, theory.getTitle());
            ps.setString(3, theory.getSlug());
            ps.setString(4, theory.getCategory());
            ps.setString(5, theory.getSummary());
            ps.setString(6, theory.getContentMarkdown());
            ps.setInt(7, theory.getOrderIndex());
            ps.setBoolean(8, theory.isPublished());
            ps.setTimestamp(9, now);
            ps.setTimestamp(10, now);

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                theory.setCreatedAt(now.toInstant());
                theory.setUpdatedAt(now.toInstant());
                return theory;
            }
        } catch (SQLException e) {
            log.error("Lỗi khi insert theory qua JDBC: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Theories update(Theories theory) {
        String sql = """
                UPDATE theories
                SET title = ?, category = ?, summary = ?, content_markdown = ?, order_index = ?, is_published = ?, updated_at = ?
                WHERE id = ?
                """;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, theory.getTitle());
            ps.setString(2, theory.getCategory());
            ps.setString(3, theory.getSummary());
            ps.setString(4, theory.getContentMarkdown());
            ps.setInt(5, theory.getOrderIndex());
            ps.setBoolean(6, theory.isPublished());
            ps.setTimestamp(7, now);
            ps.setObject(8, theory.getId());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                theory.setUpdatedAt(now.toInstant());
                return theory;
            }
        } catch (SQLException e) {
            log.error("Lỗi khi update theory qua JDBC: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean delete(UUID id) {
        String sql = "DELETE FROM theories WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Lỗi khi delete theory qua JDBC: {}", e.getMessage());
        }
        return false;
    }
}
