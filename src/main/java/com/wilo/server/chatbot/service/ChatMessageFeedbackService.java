package com.wilo.server.chatbot.service;

import com.wilo.server.chatbot.dto.ChatMessageFeedbackRequest;
import com.wilo.server.chatbot.dto.ChatMessageFeedbackResponse;
import com.wilo.server.chatbot.entity.ChatMessage;
import com.wilo.server.chatbot.entity.SenderType;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.chatbot.repository.ChatMessageRepository;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatMessageFeedbackService {

    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatMessageFeedbackResponse giveFeedback(Long messageId, ChatMessageFeedbackRequest request) {

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ApplicationException(ChatbotErrorCase.MESSAGE_NOT_FOUND));

        // BOT 메시지만 피드백 가능
        if (message.getSenderType() != SenderType.BOT) {
            throw new ApplicationException(ChatbotErrorCase.INVALID_MESSAGE_FEEDBACK);
        }

        message.updateFeedback(request.getType());

        return ChatMessageFeedbackResponse.builder()
                .messageId(message.getId())
                .feedback(request.getType().name())
                .build();
    }
}