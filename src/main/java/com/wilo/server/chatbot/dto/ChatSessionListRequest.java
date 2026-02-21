package com.wilo.server.chatbot.dto;

import lombok.Getter;

@Getter
public class ChatSessionListRequest {
    private String status;
    private Long cursor;
    private Integer size;
}