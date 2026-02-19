package com.wilo.server.chatbot.dto;

import com.wilo.server.chatbot.entity.ChatbotType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatbotTypeItem {

    private Long id;
    private String code;
    private String name;
    private String description;
    private boolean isActive;

    public static ChatbotTypeItem from(ChatbotType type) {
        return ChatbotTypeItem.builder()
                .id(type.getId())
                .code(type.getCode())
                .name(type.getName())
                .description(type.getDescription())
                .isActive(type.isActive())
                .build();
    }
}
