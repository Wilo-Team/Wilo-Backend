package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ChatSessionListResponse {
    private List<ChatSessionListItem> sessions;
    private LocalDateTime nextCursorActivityAt;
    private Long nextCursorId;
    private boolean hasNext;
}
