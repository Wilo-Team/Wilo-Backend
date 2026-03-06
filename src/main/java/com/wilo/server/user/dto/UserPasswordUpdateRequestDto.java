package com.wilo.server.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPasswordUpdateRequestDto(
        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "새 비밀번호는 8자 이상 64자 이하여야 합니다.")
        String newPassword
) {
}
