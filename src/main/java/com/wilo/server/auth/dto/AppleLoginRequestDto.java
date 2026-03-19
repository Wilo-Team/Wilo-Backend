package com.wilo.server.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AppleLoginRequestDto(
        @NotBlank(message = "Apple accessToken은 필수입니다.")
        String accessToken
) {
}
