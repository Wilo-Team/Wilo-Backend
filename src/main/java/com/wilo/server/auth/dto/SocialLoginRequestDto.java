package com.wilo.server.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequestDto(
        @NotBlank(message = "accessToken은 필수입니다.")
        String accessToken
) {
}
