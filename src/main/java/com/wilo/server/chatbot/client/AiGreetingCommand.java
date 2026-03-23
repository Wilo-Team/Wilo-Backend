package com.wilo.server.chatbot.client;

import com.wilo.server.chatbot.client.dto.AiRoleMessage;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AiGreetingCommand {
    private String requestId;
    private String userId;
    private Long sessionId;
    private String personaId;
    private String sessionSummary;
    private List<AiRoleMessage> recentMessages;
    private Map<String, Object> memory;
}