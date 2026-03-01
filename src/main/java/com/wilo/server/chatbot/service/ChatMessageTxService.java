package com.wilo.server.chatbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilo.server.chatbot.client.AiChatResult;
import com.wilo.server.chatbot.client.dto.AiRoleMessage;
import com.wilo.server.chatbot.dto.ChatMessageDto;
import com.wilo.server.chatbot.dto.ChatMessageSendRequest;
import com.wilo.server.chatbot.entity.*;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.chatbot.repository.ChatMessageRepository;
import com.wilo.server.chatbot.repository.ChatSessionMemoryRepository;
import com.wilo.server.chatbot.repository.ChatSessionRepository;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageTxService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionMemoryRepository chatSessionMemoryRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Long getPersonaIdWithAuthCheck(Long sessionId, Long userId, String guestId) {

        ChatSession session = chatSessionRepository
                .findByIdWithChatbotType(sessionId)
                .orElseThrow(() -> new ApplicationException(ChatbotErrorCase.SESSION_NOT_FOUND));

        boolean isOwner = (userId != null && session.getUserId() != null && session.getUserId().equals(userId))
                || (userId == null && guestId != null && guestId.equals(session.getGuestId()));

        if (!isOwner) {
            throw new ApplicationException(ChatbotErrorCase.SESSION_FORBIDDEN);
        }

        return session.getChatbotType().getId();
    }

    @Transactional
    public ChatMessage saveUserMessage(Long sessionId, ChatMessageSendRequest request) {

        MessageType type = (request.getMessageType() == null) ? MessageType.TEXT : request.getMessageType();

        return chatMessageRepository.save(
                ChatMessage.createUser(sessionId, type, request.getMessage())
        );
    }

    @Transactional
    public ChatMessage saveBotMessageWithSessionUpdate(Long sessionId, AiChatResult aiResult) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApplicationException(ChatbotErrorCase.SESSION_NOT_FOUND));

        String choicesJson = null;
        if (aiResult.getChoices() != null) {
            try {
                choicesJson = objectMapper.writeValueAsString(aiResult.getChoices());
            } catch (Exception e) {
                throw new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID, e);
            }
        }

        ChatMessage savedBot = chatMessageRepository.save(
                ChatMessage.createBot(
                        sessionId,
                        aiResult.getAnswer(),
                        aiResult.getSafetyStatus(),
                        choicesJson
                )
        );

        session.updateLastMessageAt(savedBot.getCreatedAt());

        return savedBot;
    }

    public ChatMessageDto toDto(ChatMessage m) {

        List<String> choices = null;

        if (m.getChoicesJson() != null && !m.getChoicesJson().isBlank()) {
            try {
                choices = objectMapper.readValue(m.getChoicesJson(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("파싱 실패 id={}", m.getId(), e);
            }
        }

        return ChatMessageDto.builder()
                .messageId(m.getId())
                .senderType(m.getSenderType())
                .messageType(m.getMessageType())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .safetyStatus(m.getSafetyStatus())
                .choices(choices)
                .attachments(List.of())
                .build();
    }

    @Transactional(readOnly = true)
    public String getPersonaCodeWithAuthCheck(
            Long sessionId,
            Long userId,
            String guestId
    ) {
        ChatSession session = chatSessionRepository
                .findByIdWithChatbotType(sessionId)
                .orElseThrow(() -> new ApplicationException(ChatbotErrorCase.SESSION_NOT_FOUND));

        boolean isOwner = (userId != null && session.getUserId() != null && session.getUserId().equals(userId))
                        || (userId == null && guestId != null && guestId.equals(session.getGuestId()));

        if (!isOwner) {
            throw new ApplicationException(ChatbotErrorCase.SESSION_FORBIDDEN);
        }
        String code = session.getChatbotType().getCode();

        return ChatbotPersona.from(code).name();
    }

    @Transactional(readOnly = true)
    public List<AiRoleMessage> findRecentAiMessages(Long sessionId, int n) {
        List<ChatMessage> recentDesc = chatMessageRepository.findRecentDesc(sessionId, PageRequest.of(0, n));
        // desc → asc로 바꿔 대화 순서 유지
        return recentDesc.reversed().stream()
                .map(m -> AiRoleMessage.builder()
                        .role(m.getSenderType() == SenderType.USER ? "user" : "assistant")
                        .content(m.getContent())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public String getSessionSummary(Long sessionId) {
        return chatSessionMemoryRepository.findById(sessionId)
                .map(ChatSessionMemory::getSummary)
                .orElse("");
    }
}