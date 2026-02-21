package com.wilo.server.chatbot.exception;

import com.wilo.server.global.exception.ErrorCase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatbotErrorCase implements ErrorCase {

    CHATBOT_INTERNAL_SERVER_ERROR(500, 3001, "서버 오류가 발생했습니다."),
    CHATBOT_TYPE_NOT_FOUND(404, 3002, "존재하지 않는 챗봇 유형입니다."),
    INVALID_PARAMETER(400, 3003, "요청 파라미터가 올바르지 않습니다."),
    GUEST_ID_REQUIRED(400, 3004, "비로그인 사용자는 X-Guest-Id 헤더가 필요합니다.");

    private final Integer httpStatusCode;
    private final Integer errorCode;
    private final String message;
}
