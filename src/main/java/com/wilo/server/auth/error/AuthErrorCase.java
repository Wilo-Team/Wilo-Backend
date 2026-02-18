package com.wilo.server.auth.error;

import com.wilo.server.global.exception.ErrorCase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCase implements ErrorCase {

    EMAIL_ALREADY_EXISTS(409, 1001, "이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(409, 1002, "이미 사용 중인 닉네임입니다."),
    INVALID_CREDENTIALS(401, 1003, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(401, 1004, "유효하지 않은 리프레시 토큰입니다."),
    UNAUTHORIZED(401, 1005, "로그인이 필요합니다.");

    private final Integer httpStatusCode;
    private final Integer errorCode;
    private final String message;
}
