package com.wilo.server.chatbot.client.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSummarizeResponse {
    private String summary;
    private List<String> key_topics;
}
