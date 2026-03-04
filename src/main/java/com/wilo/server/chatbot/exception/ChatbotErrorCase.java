package com.wilo.server.chatbot.exception;

import com.wilo.server.global.exception.ErrorCase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatbotErrorCase implements ErrorCase {

    CHATBOT_INTERNAL_SERVER_ERROR(500, 3001, "서버 오류가 발생했습니다."),
    CHATBOT_TYPE_NOT_FOUND(404, 3002, "존재하지 않는 챗봇 유형입니다."),
    INVALID_PARAMETER(400, 3003, "요청 파라미터가 올바르지 않습니다."),
    GUEST_ID_REQUIRED(400, 3004, "비로그인 사용자는 X-Guest-Id 헤더가 필요합니다."),
    SESSION_FORBIDDEN(403, 3005, "접근 권한이 없습니다."),
    SESSION_NOT_FOUND(404, 3006, "대화 세션을 찾을 수 없습니다."),
    SESSION_DELETE_INVALID(400,3007, "삭제할 세션이 올바르지 않습니다."),
    TITLE_INVALID(400, 3008, "제목이 정책을 충족하지 않습니다."),
    AI_SERVER_FAILED(502, 3009, "AI 서버 응답에 실패했습니다."),
    AI_RESPONSE_INVALID(502, 3010, "AI 응답 형식이 올바르지 않습니다."),
    MESSAGE_NOT_FOUND(404, 3011, "메시지를 찾을 수 없습니다."),
    INVALID_MESSAGE_FEEDBACK(400, 3012, "봇 메시지에만 피드백을 남길 수 있습니다.");

    private final Integer httpStatusCode;
    private final Integer errorCode;
    private final String message;
}
