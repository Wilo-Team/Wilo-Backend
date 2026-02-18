package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatbotTypeListResponse {

    private List<ChatbotTypeItem> chatbotTypes;

    public static ChatbotTypeListResponse of(List<ChatbotTypeItem> items) {
        return ChatbotTypeListResponse.builder()
                .chatbotTypes(items)
                .build();
    }
}
