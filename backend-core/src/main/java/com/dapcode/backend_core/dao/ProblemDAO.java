package com.dapcode.backend_core.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dapcode.backend_core.entity.Problem;

public interface ProblemDAO {

    List<Problem> findAll();

    Optional<Problem> findById(UUID id);

    Optional<Problem> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Problem insert(Problem problem);

    Problem update(Problem problem);

    boolean delete(UUID id);

    /**
     * Tìm kiếm, lọc và phân trang
     */
    List<Problem> findWithFilters(String category, String difficulty, String search, int page, int size);

    /**
     * Đếm tổng số bản ghi thỏa mãn điều kiện lọc
     */
    long countWithFilters(String category, String difficulty, String search);
}
