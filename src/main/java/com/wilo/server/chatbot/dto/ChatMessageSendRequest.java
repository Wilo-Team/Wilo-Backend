package com.wilo.server.chatbot.dto;

import com.wilo.server.chatbot.entity.MessageType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageSendRequest {
    private MessageType messageType;

    @NotBlank(message = "message는 필수입니다.")
    @Size(max = 4000, message = "message는 4000자 이하입니다.")
    private String message;
}
