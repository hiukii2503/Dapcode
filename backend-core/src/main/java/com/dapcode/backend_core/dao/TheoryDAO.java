package com.dapcode.backend_core.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dapcode.backend_core.entity.Theories;

public interface TheoryDAO {

    List<Theories> findAll();

    List<Theories> findByCategory(String category);

    Optional<Theories> findById(UUID id);

    Optional<Theories> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Theories insert(Theories theory);

    Theories update(Theories theory);

    boolean delete(UUID id);
}

