package com.wilo.server.chatbot.repository;

import com.wilo.server.chatbot.entity.ChatSuggestionPreset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatSuggestionPresetRepository extends JpaRepository<ChatSuggestionPreset, Long> {
    List<ChatSuggestionPreset> findAllByChatbotType_IdAndIsActiveTrueOrderBySortOrderAsc(Long chatbotTypeId);
}