package com.wilo.server.chatbot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ChatSessionCreateRequest {

    @NotNull(message = "chatbotTypeId는 필수입니다.")
    private Long chatbotTypeId;
}
