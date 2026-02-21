package com.wilo.server.chatbot.service;

import com.wilo.server.chatbot.dto.ChatbotTypeItem;
import com.wilo.server.chatbot.dto.ChatbotTypeListResponse;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.chatbot.repository.ChatbotTypeRepository;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatbotTypeService {

    private final ChatbotTypeRepository chatbotTypeRepository;

    public ChatbotTypeListResponse getChatbotTypes() {
        try {
            List<ChatbotTypeItem> items = chatbotTypeRepository
                    .findAllByIsActiveTrueOrderByIdAsc()
                    .stream()
                    .map(ChatbotTypeItem::from)
                    .toList();

            return ChatbotTypeListResponse.of(items);

        } catch (Exception e) {
            throw new ApplicationException(ChatbotErrorCase.CHATBOT_INTERNAL_SERVER_ERROR, e);
        }
    }
}
