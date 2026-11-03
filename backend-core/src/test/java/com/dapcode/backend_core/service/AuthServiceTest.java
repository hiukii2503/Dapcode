package com.dapcode.backend_core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dapcode.backend_core.common.ErrorCode;
import com.dapcode.backend_core.config.JwtTokenProvider;
import com.dapcode.backend_core.dto.auth.AdminLoginRequest;
import com.dapcode.backend_core.dto.auth.AuthResponse;
import com.dapcode.backend_core.entity.Users;
import com.dapcode.backend_core.exception.AppException;
import com.dapcode.backend_core.repository.UserRepository;
import com.dapcode.backend_core.service.auth.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private Users testAdmin;

    @BeforeEach
    void setUp() {
        testAdmin = Users.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@dapcode.io")
                .passwordHash("admin123")
                .role("ROLE_ADMIN")
                .isActive(true)
                .build();
    }

    @Test
    void testAdminLogin_Success() {
        AdminLoginRequest req = AdminLoginRequest.builder()
                .usernameOrEmail("admin@dapcode.io")
                .password("admin123")
                .build();

        when(userRepository.findByEmail("admin@dapcode.io")).thenReturn(Optional.of(testAdmin));
        when(jwtTokenProvider.generateToken(any(Users.class))).thenReturn("mock-jwt-token-xyz");

        AuthResponse response = authService.authenticateAdmin(req);

        assertNotNull(response);
        assertEquals("mock-jwt-token-xyz", response.getToken());
        assertEquals("ROLE_ADMIN", response.getUser().getRole());
        assertEquals("admin", response.getUser().getUsername());
    }

    @Test
    void testAdminLogin_WrongPassword() {
        AdminLoginRequest req = AdminLoginRequest.builder()
                .usernameOrEmail("admin@dapcode.io")
                .password("wrongpassword")
                .build();

        AppException ex = assertThrows(AppException.class, () -> authService.authenticateAdmin(req));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    }

    @Test
    void testAdminLogin_UserLocked() {
        testAdmin.setActive(false);
        AdminLoginRequest req = AdminLoginRequest.builder()
                .usernameOrEmail("admin@dapcode.io")
                .password("admin123")
                .build();

        when(userRepository.findByEmail("admin@dapcode.io")).thenReturn(Optional.of(testAdmin));

        AppException ex = assertThrows(AppException.class, () -> authService.authenticateAdmin(req));
        assertEquals(ErrorCode.USER_LOCKED, ex.getErrorCode());
    }
}

