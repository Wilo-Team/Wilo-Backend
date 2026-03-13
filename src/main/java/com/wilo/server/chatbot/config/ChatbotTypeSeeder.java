package com.wilo.server.chatbot.config;

import com.wilo.server.chatbot.entity.ChatbotType;
import com.wilo.server.chatbot.repository.ChatbotTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Order(1)
@Transactional
public class ChatbotTypeSeeder implements ApplicationRunner {

    private final ChatbotTypeRepository chatbotTypeRepository;

    @Override
    public void run(ApplicationArguments args) {

        seed("EUNHAENG", "편안한 친구", "지금의 상태를 먼저 읽어주는 존재",
                "https://cdn.wilo.com/chatbot/eunhaeng.png","#FCEBB2",
                "#F4C008");

        seed("BUDDLE", "따뜻한 조력자", "선택을 대신하지 않고 정리해주는 존재",
                "https://cdn.wilo.com/chatbot/buddle.png","#B0E7EE",
                "#00B3C9");

        seed("NEUTY", "안전한 동반자", "혼자가 되지 않도록 곁을 지키는 존재",
                "https://cdn.wilo.com/chatbot/neuty.png","#F35A38",
                "#FBCCC1");
    }

    private void seed(
            String code,
            String name,
            String desc,
            String imageUrl,
            String backgroundColor,
            String borderColor
    ) {
        if (chatbotTypeRepository.existsByCode(code)) {
            return;
        }

        try {
            chatbotTypeRepository.save(
                    ChatbotType.create(
                            code,
                            name,
                            desc,
                            imageUrl,
                            true,
                            backgroundColor,
                            borderColor
                    )
            );
        } catch (DataIntegrityViolationException ignored) {
        }
    }
}
