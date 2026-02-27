package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageSendResponse {
    private Long sessionId;
    private ChatMessageDto userMessage;
    private ChatMessageDto botMessage;
}
