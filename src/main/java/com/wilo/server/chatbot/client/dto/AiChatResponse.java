package com.wilo.server.chatbot.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatResponse {
    @JsonProperty("request_id")
    private String requestId;
    private String persona;
    private String answer;
    private List<String> choices;
    @JsonProperty("safety_status")
    private String safetyStatus;   // "clear" | "monitor" | "caution" | "crisis"
    private List<Citation> citations; // 항상 배열
    private String status;
    @JsonProperty("error_type")
    private String errorType;
    @JsonProperty("memory_patch")
    private Map<String, Object> memoryPatch; // null 가능

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Citation {
        @JsonProperty("doc_id")
        private String docId;
        @JsonProperty("chunk_id")
        private String chunkId;
        private String title;
        private String source;
        private Double similarity;
        private String snippet;
    }
}
