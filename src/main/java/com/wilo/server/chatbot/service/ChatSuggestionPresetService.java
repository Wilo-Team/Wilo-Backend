package com.wilo.server.chatbot.service;

import com.wilo.server.chatbot.dto.SuggestionPresetItem;
import com.wilo.server.chatbot.dto.SuggestionPresetListResponse;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.chatbot.repository.ChatSuggestionPresetRepository;
import com.wilo.server.chatbot.repository.ChatbotTypeRepository;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatSuggestionPresetService {

    private final ChatbotTypeRepository chatbotTypeRepository;
    private final ChatSuggestionPresetRepository presetRepository;

    public SuggestionPresetListResponse getPresets(Long chatbotTypeId) {

        // 타입 존재 검증
        chatbotTypeRepository.findById(chatbotTypeId)
                .orElseThrow(() -> new ApplicationException(ChatbotErrorCase.CHATBOT_TYPE_NOT_FOUND));

        List<SuggestionPresetItem> items = presetRepository
                .findAllByChatbotType_IdAndIsActiveTrueOrderBySortOrderAsc(chatbotTypeId)
                .stream()
                .map(p -> SuggestionPresetItem.builder()
                        .id(p.getId())
                        .text(p.getText())
                        .order(p.getSortOrder())
                        .build())
                .toList();

        return SuggestionPresetListResponse.of(chatbotTypeId, items);
    }
}
