package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatSessionArchiveResponse {
    private Long sessionId;
    private String status;
}
