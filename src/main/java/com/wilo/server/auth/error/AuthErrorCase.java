package com.wilo.server.auth.error;

import com.wilo.server.global.exception.ErrorCase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCase implements ErrorCase {

    EMAIL_ALREADY_EXISTS(409, 1001, "이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(409, 1002, "이미 사용 중인 닉네임입니다."),
    INVALID_CREDENTIALS(401, 1003, "전화번호 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(401, 1004, "유효하지 않은 리프레시 토큰입니다."),
    UNAUTHORIZED(401, 1005, "로그인이 필요합니다."),
    SMS_CONFIGURATION_MISSING(500, 1006, "문자 발송 설정이 누락되었습니다."),
    SMS_SEND_FAILED(500, 1007, "인증 문자 발송에 실패했습니다."),
    PHONE_VERIFICATION_CODE_EXPIRED(400, 1008, "인증번호가 만료되었거나 존재하지 않습니다."),
    PHONE_VERIFICATION_CODE_MISMATCH(400, 1009, "인증번호가 일치하지 않습니다."),
    PHONE_ALREADY_EXISTS(409, 1010, "이미 사용 중인 전화번호입니다."),
    APPLE_ACCESS_TOKEN_INVALID(401, 1011, "유효하지 않은 Apple access token 입니다."),
    APPLE_CONFIG_MISSING(500, 1012, "Apple 로그인 설정이 누락되었습니다."),
    APPLE_TOKEN_EXCHANGE_FAILED(400, 1013, "Apple 토큰 교환에 실패했습니다."),
    APPLE_TOKEN_REVOKE_FAILED(500, 1014, "Apple 토큰 철회에 실패했습니다.");

    private final Integer httpStatusCode;
    private final Integer errorCode;
    private final String message;
}
