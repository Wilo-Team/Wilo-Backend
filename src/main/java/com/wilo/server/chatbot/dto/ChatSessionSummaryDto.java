package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatSessionSummaryDto {
    private Long sessionId;
    private String title;
    private String status;
    private ChatbotTypeDto chatbotType;
}
