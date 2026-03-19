package com.wilo.server.chatbot.client;

import com.wilo.server.chatbot.entity.SafetyStatus;

public class AiSafetyMapper {
    private AiSafetyMapper() {}

    public static SafetyStatus toBackend(String safetyStatus) {
        if (safetyStatus == null) return null;
        return switch (safetyStatus.toLowerCase()) {
            case "clear" -> SafetyStatus.SAFE;
            case "monitor", "caution" -> SafetyStatus.WARNING;
            case "crisis" -> SafetyStatus.CRITICAL;
            default -> null;
        };
    }
}
