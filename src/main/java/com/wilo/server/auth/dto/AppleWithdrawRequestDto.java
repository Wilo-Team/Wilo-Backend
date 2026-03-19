package com.wilo.server.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AppleWithdrawRequestDto(
        @NotBlank(message = "Apple authorizationCode는 필수입니다.")
        String authorizationCode
) {
}
