package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatSessionDetailResponse {

    private ChatSessionSummaryDto session;
    private List<ChatMessageDto> messages;
    private ChatSessionMemorySummaryDto summary;

    private Long nextCursor;
    private boolean hasNext;
}
