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
import com.dapcode.backend_core.entity.TestCase;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class TestCaseDAOImpl implements TestCaseDAO {

    private TestCase mapResultSetToTestCase(ResultSet rs) throws SQLException {
        UUID problemId = (UUID) rs.getObject("problem_id");
        Problem problem = Problem.builder().id(problemId).build();

        return TestCase.builder()
                .id((UUID) rs.getObject("id"))
                .problem(problem)
                .inputData(rs.getString("input_data"))
                .expectedOutput(rs.getString("expected_output"))
                .isHidden(rs.getBoolean("is_hidden"))
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null)
                .build();
    }

    @Override
    public List<TestCase> findByProblemId(UUID problemId) {
        List<TestCase> list = new ArrayList<>();
        String sql = "SELECT * FROM test_cases WHERE problem_id = ? ORDER BY created_at ASC";
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, problemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTestCase(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi khi findByProblemId test cases qua JDBC: {}", e.getMessage());
        }
        return list;
    }

    @Override
    public Optional<TestCase> findById(UUID id) {
        String sql = "SELECT * FROM test_cases WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTestCase(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi khi findById test case qua JDBC: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public TestCase insert(TestCase testCase) {
        String sql = """
                INSERT INTO test_cases (id, problem_id, input_data, expected_output, is_hidden, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        UUID newId = testCase.getId() != null ? testCase.getId() : UUID.randomUUID();
        testCase.setId(newId);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, newId);
            ps.setObject(2, testCase.getProblem() != null ? testCase.getProblem().getId() : null);
            ps.setString(3, testCase.getInputData());
            ps.setString(4, testCase.getExpectedOutput());
            ps.setBoolean(5, testCase.isHidden());
            ps.setTimestamp(6, now);

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                testCase.setCreatedAt(now.toInstant());
                return testCase;
            }
        } catch (SQLException e) {
            log.error("Lỗi khi insert test case qua JDBC: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public TestCase update(TestCase testCase) {
        String sql = """
                UPDATE test_cases
                SET input_data = ?, expected_output = ?, is_hidden = ?
                WHERE id = ?
                """;
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, testCase.getInputData());
            ps.setString(2, testCase.getExpectedOutput());
            ps.setBoolean(3, testCase.isHidden());
            ps.setObject(4, testCase.getId());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                return testCase;
            }
        } catch (SQLException e) {
            log.error("Lỗi khi update test case qua JDBC: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean delete(UUID id) {
        String sql = "DELETE FROM test_cases WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Lỗi khi delete test case qua JDBC: {}", e.getMessage());
        }
        return false;
    }
}
