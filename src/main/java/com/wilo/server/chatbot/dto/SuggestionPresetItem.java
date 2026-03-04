package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SuggestionPresetItem {
    private Long id;
    private String text;
    private int order;
}
