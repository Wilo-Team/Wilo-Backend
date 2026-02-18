package com.wilo.server.chatbot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ChatSessionCreateRequest {

    @NotNull(message = "chatbotTypeId는 필수입니다.")
    private Long chatbotTypeId;

    // 비로그인 사용자를 위한 식별자(없으면 서버가 생성해서 내려줌)
    private String guestId;
}
