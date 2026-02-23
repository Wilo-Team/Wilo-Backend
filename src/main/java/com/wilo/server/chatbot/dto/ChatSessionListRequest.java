package com.wilo.server.chatbot.dto;

import java.time.LocalDateTime;

public record ChatSessionListRequest(
        String status,
        LocalDateTime cursorLastMessageAt,
        Long cursorId,
        Integer size
) {}