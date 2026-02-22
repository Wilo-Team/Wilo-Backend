package com.wilo.server.chatbot.dto;

public record ChatSessionListRequest(
        String status,
        Long cursor,
        Integer size
) { }