package com.wilo.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneVerificationConfirmRequestDto(
        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(
                regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다."
        )
        String phoneNumber,

        @NotBlank(message = "인증번호는 필수입니다.")
        @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자여야 합니다.")
        String verificationCode
) {
}
