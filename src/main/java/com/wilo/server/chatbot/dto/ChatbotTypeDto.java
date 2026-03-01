package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatbotTypeDto {
    private Long id;
    private String name;
}