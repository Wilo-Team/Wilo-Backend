package com.wilo.server.chatbot.dto;


import com.wilo.server.chatbot.entity.ChatSession;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatSessionCreateResponse {

    private Long sessionId;
    private Long chatbotTypeId;
    private String title;
    private String status;

    // 비로그인일 때만 내려주기
    private String guestId;

    public static ChatSessionCreateResponse from(ChatSession session) {
        return ChatSessionCreateResponse.builder()
                .sessionId(session.getId())
                .chatbotTypeId(session.getChatbotType().getId())
                .title(session.getTitle())
                .status(session.getStatus().name())
                .guestId(session.getGuestId())
                .build();
    }
}
