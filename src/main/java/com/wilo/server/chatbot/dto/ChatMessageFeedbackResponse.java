package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageFeedbackResponse {
    private Long messageId;
    private String feedback;
}
