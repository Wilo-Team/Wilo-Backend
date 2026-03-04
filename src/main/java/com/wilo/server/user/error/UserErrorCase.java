package com.wilo.server.user.error;

import com.wilo.server.global.exception.ErrorCase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCase implements ErrorCase {

    USER_NOT_FOUND(404, 3001, "유저를 찾을 수 없습니다."),
    NICKNAME_ALREADY_EXISTS(409, 3002, "이미 사용 중인 닉네임입니다."),
    INVALID_NICKNAME(400, 3003, "닉네임이 정책을 충족하지 않습니다."),
    GUEST_PROFILE_NOT_FOUND(404,3004,"게스트 프로필을 찾을 수 없습니다."),
    INVALID_GUEST_ID(400,3005,"게스트 ID의 형식이 올바르지 않습니다."),
    CURRENT_PASSWORD_MISMATCH(400, 3006, "현재 비밀번호가 일치하지 않습니다."),
    SAME_AS_CURRENT_PASSWORD(400, 3007, "새 비밀번호는 현재 비밀번호와 달라야 합니다.");

    private final Integer httpStatusCode;
    private final Integer errorCode;
    private final String message;
}
