package com.wilo.server.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AppleLoginRequestDto(
        @NotBlank(message = "Apple authorizationCode는 필수입니다.")
        String authorizationCode,
        @NotBlank(message = "Apple identityToken은 필수입니다.")
        String identityToken
) {
}
