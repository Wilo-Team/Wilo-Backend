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
    private String imageUrl;
    private boolean active;
    private String backgroundColor;
    private String borderColor;

    public static ChatbotTypeItem from(ChatbotType type) {
        return ChatbotTypeItem.builder()
                .id(type.getId())
                .code(type.getCode())
                .name(type.getName())
                .description(type.getDescription())
                .imageUrl(type.getImageUrl())
                .active(type.isActive())
                .backgroundColor(type.getBackgroundColor())
                .borderColor(type.getBorderColor())
                .build();
    }
}
