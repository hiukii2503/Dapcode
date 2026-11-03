package com.dapcode.backend_core.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dapcode.backend_core.entity.TestCase;

public interface TestCaseDAO {

    List<TestCase> findByProblemId(UUID problemId);

    Optional<TestCase> findById(UUID id);

    TestCase insert(TestCase testCase);

    TestCase update(TestCase testCase);

    boolean delete(UUID id);
}

