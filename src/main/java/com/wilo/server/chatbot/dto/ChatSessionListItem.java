package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ChatSessionListItem {
    private Long sessionId;
    private String title;
    private ChatbotTypeDto chatbotType;
    private String status;
    private LocalDateTime lastMessageAt;
    private String preview;
}
