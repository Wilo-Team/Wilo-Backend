package com.wilo.server.chatbot.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SuggestionPresetListResponse {
    private Long chatbotTypeId;
    private List<SuggestionPresetItem> presets;

    public static SuggestionPresetListResponse of(Long chatbotTypeId, List<SuggestionPresetItem> presets) {
        return SuggestionPresetListResponse.builder()
                .chatbotTypeId(chatbotTypeId)
                .presets(presets)
                .build();
    }
}
