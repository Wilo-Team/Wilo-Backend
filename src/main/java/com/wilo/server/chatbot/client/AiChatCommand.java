package com.wilo.server.chatbot.client;

import com.wilo.server.chatbot.client.dto.AiRoleMessage;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AiChatCommand {
    private String requestId;
    private String userId;
    private Long sessionId;

    private String personaId; // "EUNHAENG" | "BUDDLE" | "NEUTY"
    private String message;

    private String sessionSummary;            // context.session_summary
    private List<AiRoleMessage> recentMessages; // context.recent_messages
    private Map<String, Object> memory;
}
