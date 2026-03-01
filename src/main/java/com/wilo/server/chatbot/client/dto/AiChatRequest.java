package com.wilo.server.chatbot.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor @Builder
public class AiChatRequest {
    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("persona_id")
    private String personaId;

    private String message;
    private Context context;
    private String persona; // legacy (optional)

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Context {
        @JsonProperty("recent_messages")
        private List<AiRoleMessage> recentMessages;
        @JsonProperty("session_summary")
        private String sessionSummary;
        private Map<String, Object> memory;
    }
}
