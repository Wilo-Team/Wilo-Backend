package com.wilo.server.chatbot.dto;

import com.wilo.server.chatbot.entity.MessageFeedbackType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ChatMessageFeedbackRequest {
    @NotNull
    private MessageFeedbackType type;
}
