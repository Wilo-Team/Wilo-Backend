package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ChatMessageDto {
    private Long messageId;
    private String senderType;
    private String messageType;
    private String content;
    private LocalDateTime createdAt;

    // 봇 전용(없으면 null)
    private String safetyStatus;
    private List<String> choices;

    private List<ChatMessageAttachmentDto> attachments;
}
