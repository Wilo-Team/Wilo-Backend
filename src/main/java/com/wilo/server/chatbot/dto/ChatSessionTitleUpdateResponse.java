package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatSessionTitleUpdateResponse {
    private Long sessionId;
    private String title;
}
