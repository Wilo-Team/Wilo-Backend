package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatSessionListResponse {
    private List<ChatSessionListItem> sessions;
    private Long nextCursor;
    private boolean hasNext;
}
