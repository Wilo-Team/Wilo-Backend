package com.wilo.server.chatbot.dto;

import java.time.LocalDateTime;

public record ChatSessionListRequest(
        String status,
        LocalDateTime cursorActivityAt,
        Long cursorId,
        Integer size
) {}