package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageAttachmentDto {
    private Long mediaId;
    private String url;
    private String mediaType;
}
