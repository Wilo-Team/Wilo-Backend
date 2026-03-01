package com.wilo.server.chatbot.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class ChatSessionDeleteRequest {
    @NotEmpty
    private List<Long> sessionIds;
}
