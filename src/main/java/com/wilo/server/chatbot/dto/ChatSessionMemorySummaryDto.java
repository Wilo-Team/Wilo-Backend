package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ChatSessionMemorySummaryDto {
    private boolean exists;
    private String text;
    private LocalDateTime updatedAt;
}
