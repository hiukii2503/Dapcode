package com.dapcode.backend_core.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.dapcode.backend_core.entity.Users;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class UserDAOImpl implements UserDAO {

    private Users mapResultSetToUser(ResultSet rs) throws SQLException {
        return Users.builder()
                .id((UUID) rs.getObject("id"))
                .username(rs.getString("username"))
                .email(rs.getString("email"))
                .passwordHash(rs.getString("password_hash"))
                .role(rs.getString("role"))
                .isActive(rs.getBoolean("is_active"))
                .xp(rs.getInt("xp"))
                .streakCount(rs.getInt("streak_count"))
                .avatarUrl(rs.getString("avatar_url"))
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : null)
                .build();
    }

    @Override
    public Optional<Users> findById(UUID id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi khi findById user qua JDBC: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Users> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi khi findByEmail user qua JDBC: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Users> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi khi findByUsername user qua JDBC: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Lỗi khi check existsByEmail user qua JDBC: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Lỗi khi check existsByUsername user qua JDBC: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public Users insert(Users user) {
        String sql = """
                INSERT INTO users (id, username, email, password_hash, role, is_active, xp, streak_count, avatar_url, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        UUID newId = user.getId() != null ? user.getId() : UUID.randomUUID();
        user.setId(newId);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, newId);
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getRole() != null ? user.getRole() : "ROLE_USER");
            ps.setBoolean(6, user.isActive());
            ps.setInt(7, user.getXp());
            ps.setInt(8, user.getStreakCount());
            ps.setString(9, user.getAvatarUrl());
            ps.setTimestamp(10, now);
            ps.setTimestamp(11, now);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                user.setCreatedAt(now.toInstant());
                user.setUpdatedAt(now.toInstant());
                return user;
            }
        } catch (SQLException e) {
            log.error("Lỗi khi insert user qua JDBC: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Users update(Users user) {
        String sql = """
                UPDATE users
                SET username = ?, email = ?, password_hash = ?, role = ?, is_active = ?, xp = ?, streak_count = ?, avatar_url = ?, updated_at = ?
                WHERE id = ?
                """;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole());
            ps.setBoolean(5, user.isActive());
            ps.setInt(6, user.getXp());
            ps.setInt(7, user.getStreakCount());
            ps.setString(8, user.getAvatarUrl());
            ps.setTimestamp(9, now);
            ps.setObject(10, user.getId());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                user.setUpdatedAt(now.toInstant());
                return user;
            }
        } catch (SQLException e) {
            log.error("Lỗi khi update user qua JDBC: {}", e.getMessage());
        }
        return null;
    }
}
