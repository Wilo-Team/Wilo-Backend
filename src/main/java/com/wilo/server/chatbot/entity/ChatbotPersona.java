package com.wilo.server.chatbot.entity;

public enum ChatbotPersona {
    EUNHAENG,
    BUDDLE,
    NEUTY;

    public static ChatbotPersona from(String code) {
        try {
            return ChatbotPersona.valueOf(code);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid persona: " + code);
        }
    }
}
