package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatGreetingResponse {
    private Long sessionId;
    private ChatMessageDto botMessage;
}
