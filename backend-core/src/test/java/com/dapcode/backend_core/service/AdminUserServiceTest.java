package com.dapcode.backend_core.service;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dapcode.backend_core.common.ErrorCode;
import com.dapcode.backend_core.dto.admin.AdminUserResponse;
import com.dapcode.backend_core.dto.admin.UpdateUserRoleRequest;
import com.dapcode.backend_core.dto.admin.UpdateUserStatusRequest;
import com.dapcode.backend_core.entity.Users;
import com.dapcode.backend_core.exception.AppException;
import com.dapcode.backend_core.repository.UserRepository;
import com.dapcode.backend_core.service.user.AdminUserServiceImpl;

@ExtendWith(MockitoExtension.class)
public class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private Users currentAdmin;
    private Users targetUser;
    private UUID targetUserId;

    @BeforeEach
    void setUp() {
        currentAdmin = Users.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .role("ROLE_ADMIN")
                .isActive(true)
                .build();

        targetUserId = UUID.randomUUID();
        targetUser = Users.builder()
                .id(targetUserId)
                .username("student1")
                .email("student1@dapcode.io")
                .role("ROLE_USER")
                .isActive(true)
                .build();
    }

    @Test
    void testGetUserById_Success() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        AdminUserResponse res = adminUserService.getUserById(targetUserId);

        assertNotNull(res);
        assertEquals("student1", res.getUsername());
        assertEquals("ROLE_USER", res.getRole());
    }

    @Test
    void testUpdateUserStatus_LockUser_Success() {
        UpdateUserStatusRequest req = UpdateUserStatusRequest.builder()
                .isActive(false)
                .bannedReason("Vi phạm quy chế thi")
                .build();

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(Users.class))).thenAnswer(i -> i.getArgument(0));

        AdminUserResponse res = adminUserService.updateUserStatus(targetUserId, req, currentAdmin);

        assertNotNull(res);
        assertEquals(false, res.isActive());
        assertEquals("Vi phạm quy chế thi", res.getBannedReason());
        verify(userRepository).save(targetUser);
    }

    @Test
    void testUpdateUserStatus_SelfLock_Forbidden() {
        UpdateUserStatusRequest req = UpdateUserStatusRequest.builder()
                .isActive(false)
                .bannedReason("Tự khóa")
                .build();

        when(userRepository.findById(currentAdmin.getId())).thenReturn(Optional.of(currentAdmin));

        AppException ex = assertThrows(AppException.class,
                () -> adminUserService.updateUserStatus(currentAdmin.getId(), req, currentAdmin));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void testUpdateUserRole_Success() {
        UpdateUserRoleRequest req = UpdateUserRoleRequest.builder()
                .role("ROLE_ADMIN")
                .build();

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(Users.class))).thenAnswer(i -> i.getArgument(0));

        AdminUserResponse res = adminUserService.updateUserRole(targetUserId, req, currentAdmin);

        assertNotNull(res);
        assertEquals("ROLE_ADMIN", res.getRole());
        verify(userRepository).save(targetUser);
    }
}
