package com.wilo.server.chatbot.dto;

import com.wilo.server.chatbot.entity.MessageType;
import com.wilo.server.chatbot.entity.SafetyStatus;
import com.wilo.server.chatbot.entity.SenderType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ChatMessageDto {
    private Long messageId;
    private SenderType senderType;
    private MessageType messageType;
    private String content;
    private LocalDateTime createdAt;

    // 봇 전용(없으면 null)
    private SafetyStatus safetyStatus;
    private List<String> choices;

    private List<ChatMessageAttachmentDto> attachments;
}
