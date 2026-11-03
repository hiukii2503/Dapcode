package com.dapcode.backend_core.dao;

import java.util.Optional;
import java.util.UUID;

import com.dapcode.backend_core.entity.Users;

public interface UserDAO {

    Optional<Users> findById(UUID id);

    Optional<Users> findByEmail(String email);

    Optional<Users> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Users insert(Users user);

    Users update(Users user);
}

