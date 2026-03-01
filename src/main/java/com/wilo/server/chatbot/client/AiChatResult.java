package com.wilo.server.chatbot.client;

import com.wilo.server.chatbot.entity.SafetyStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiChatResult {
    private Long sessionId;
    private String answer;
    private List<String> choices;
    private SafetyStatus safetyStatus;
}
