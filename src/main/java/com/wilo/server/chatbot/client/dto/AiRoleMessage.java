package com.wilo.server.chatbot.client.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRoleMessage {
    private String role;

    // 실제 메시지 내용
    private String content;
}
