package com.wilo.server.chatbot.client;

import com.wilo.server.chatbot.entity.SafetyStatus;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.global.exception.ApplicationException;

import java.util.List;
import java.util.Map;

public class AiResponseMapper {

    private AiResponseMapper() {}

    public static AiChatResult toResult(Map<String, Object> res) {

        Object answerObj = res.get("answer");
        Object safetyObj = res.get("safety_status");
        Object choicesObj = res.get("choices");
        Object sessionIdObj = res.get("session_id");

        if (!(answerObj instanceof String answer) || answer.isBlank()) {
            throw new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID);
        }

        SafetyStatus safetyStatus = null;
        if (safetyObj != null) {
            if (!(safetyObj instanceof String s)) {
                throw new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID);
            }
            try {
                safetyStatus = SafetyStatus.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID);
            }
        }

        List<String> choices = null;
        if (choicesObj != null) {
            if (!(choicesObj instanceof List<?> rawList)) {
                throw new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID);
            }
            choices = rawList.stream()
                    .map(v -> (v instanceof String str) ? str : null)
                    .toList();

            if (choices.stream().anyMatch(v -> v == null)) {
                throw new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID);
            }
        }

        Long sessionId = null;
        if (sessionIdObj instanceof Number n) {
            sessionId = n.longValue();
        }

        return AiChatResult.builder()
                .sessionId(sessionId)
                .answer(answer)
                .choices(choices)
                .safetyStatus(safetyStatus)
                .build();
    }
}