package com.wilo.server.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ChatSessionTitleUpdateRequest {
    @NotBlank(message = "제목은 비어 있을 수 없습니다.")
    @Size(max = 200, message = "제목은 200자 이하입니다.")
    private String title;
}
