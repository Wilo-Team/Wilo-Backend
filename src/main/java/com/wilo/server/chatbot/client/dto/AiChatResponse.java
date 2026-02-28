package com.wilo.server.chatbot.client.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatResponse {
    private String request_id;
    private String persona;
    private String answer;
    private List<String> choices;
    private String safety_status;   // "clear" | "monitor" | "caution" | "crisis"
    private List<Citation> citations; // 항상 배열
    private String status;
    private String error_type;
    private Map<String, Object> memory_patch; // null 가능

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Citation {
        private String doc_id;
        private String chunk_id;
        private String title;
        private String source;
        private Double similarity;
        private String snippet;
    }
}
