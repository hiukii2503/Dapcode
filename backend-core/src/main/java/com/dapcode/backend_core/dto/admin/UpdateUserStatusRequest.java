package com.dapcode.backend_core.dto.admin;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequest {
    @NotNull(message = "Trạng thái tài khoản (isActive) không được để trống")
    @JsonAlias("active")
    private Boolean isActive;

    private String bannedReason;
}
