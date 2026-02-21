package com.wilo.server.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequestDto(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하여야 합니다.")
        String nickname,

        @Size(max = 120, message = "한 줄 소개는 120자 이하여야 합니다.")
        String description
) {
}
