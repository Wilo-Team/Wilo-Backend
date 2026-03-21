package com.wilo.server.chatbot.client;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class AiGreetingCommand {
    private String requestId;
    private String userId;
    private Long sessionId;
    private String personaId;
    private String sessionSummary;
    private java.util.List<com.wilo.server.chatbot.client.dto.AiRoleMessage> recentMessages;
    private Map<String, Object> memory;
}