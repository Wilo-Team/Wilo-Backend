package com.wilo.server.chatbot.config;

import com.wilo.server.chatbot.entity.ChatSuggestionPreset;
import com.wilo.server.chatbot.repository.ChatSuggestionPresetRepository;
import com.wilo.server.chatbot.repository.ChatbotTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Order(2)
public class ChatSuggestionPresetSeeder implements ApplicationRunner {

    private final ChatbotTypeRepository chatbotTypeRepository;
    private final ChatSuggestionPresetRepository presetRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        seed( "EUNHAENG", 1, "내 감정을 더 잘 이해하고 싶어");
        seed( "EUNHAENG", 2, "현재의 문제를 해결할 방법을 알고 싶어");
        seed( "EUNHAENG", 3, "당장 진정하는 방법이 필요해");

        seed( "BUDDLE", 1, "그냥 편하게 얘기하고 싶어");
        seed( "BUDDLE", 2, "오늘 있었던 일 들어줄래?");
        seed( "BUDDLE", 3, "내가 너무 예민한 걸까?");

        seed( "NEUTY", 1, "상황을 정리해서 말해볼게");
        seed( "NEUTY", 2, "선택지를 정리해줘");
        seed( "NEUTY", 3, "우선순위를 잡고 싶어");
    }

    private void seed(String chatbotCode, int order, String text) {

        var type = chatbotTypeRepository.findByCode(chatbotCode)
                .orElseThrow();

        // 같은 타입+문구가 이미 있으면 스킵
        boolean exists = presetRepository
                .findAllByChatbotType_IdAndIsActiveTrueOrderBySortOrderAsc(type.getId())
                .stream().anyMatch(p -> p.getText().equals(text));

        if (exists) return;

        presetRepository.save(ChatSuggestionPreset.create(type, text, order, true));
    }
}
