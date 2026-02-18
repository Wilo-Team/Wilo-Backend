package com.wilo.server.chatbot.exception;

import com.wilo.server.global.exception.ErrorCase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatbotErrorCase implements ErrorCase {

    CHATBOT_INTERNAL_SERVER_ERROR(500, 3500, "서버 오류가 발생했습니다.");

    private final Integer httpStatusCode;
    private final Integer errorCode;
    private final String message;
}
