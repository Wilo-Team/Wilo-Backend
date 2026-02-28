package com.wilo.server.chatbot.client.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSummarizeRequest {
    private List<AiRoleMessage> messages;
}
