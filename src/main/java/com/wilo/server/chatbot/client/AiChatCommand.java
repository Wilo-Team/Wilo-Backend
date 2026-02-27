package com.wilo.server.chatbot.client;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiChatCommand {
    private String userId;      // userId or guestId 문자열
    private Long sessionId;
    private Long personaId;     // chatbotTypeId
    private String message;
}
