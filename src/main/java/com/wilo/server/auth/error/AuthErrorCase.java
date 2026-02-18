package com.wilo.server.auth.error;

import com.wilo.server.global.exception.ErrorCase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCase implements ErrorCase {

    EMAIL_ALREADY_EXISTS(409, 4091, "이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(409, 4092, "이미 사용 중인 닉네임입니다."),
    INVALID_CREDENTIALS(401, 4011, "이메일 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(401, 4012, "로그인이 필요합니다.");

    private final Integer httpStatusCode;
    private final Integer errorCode;
    private final String message;
}
