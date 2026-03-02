package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatSessionDeleteResponse {
    private int deletedCount;

    public static ChatSessionDeleteResponse of(int deletedCount) {
        return ChatSessionDeleteResponse.builder()
                .deletedCount(deletedCount)
                .build();
    }
}
