package com.wilo.server.chatbot.client.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor @Builder
public class AiChatRequest {
    private String request_id;
    private String user_id;
    private String session_id;
    private String persona_id;
    private String message;
    private Context context;
    private String persona; // legacy (optional)

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Context {
        private List<AiRoleMessage> recent_messages;
        private String session_summary;
        private Map<String, Object> memory;
    }
}
