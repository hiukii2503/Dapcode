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
}
