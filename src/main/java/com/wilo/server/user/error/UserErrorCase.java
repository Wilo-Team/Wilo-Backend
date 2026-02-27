package com.wilo.server.user.error;

import com.wilo.server.global.exception.ErrorCase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCase implements ErrorCase {

    USER_NOT_FOUND(404, 3001, "유저를 찾을 수 없습니다."),
    NICKNAME_ALREADY_EXISTS(409, 3002, "이미 사용 중인 닉네임입니다."),
    INVALID_NICKNAME(400, 3003, "닉네임이 정책을 충족하지 않습니다.");

    private final Integer httpStatusCode;
    private final Integer errorCode;
    private final String message;
}
