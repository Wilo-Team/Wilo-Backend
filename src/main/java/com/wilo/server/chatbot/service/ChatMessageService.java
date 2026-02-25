package com.wilo.server.chatbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilo.server.chatbot.client.AiChatClient;
import com.wilo.server.chatbot.client.AiChatCommand;
import com.wilo.server.chatbot.client.AiChatResult;
import com.wilo.server.chatbot.dto.ChatMessageDto;
import com.wilo.server.chatbot.dto.ChatMessageSendRequest;
import com.wilo.server.chatbot.dto.ChatMessageSendResponse;
import com.wilo.server.chatbot.entity.ChatMessage;
import com.wilo.server.chatbot.entity.ChatSession;
import com.wilo.server.chatbot.entity.MessageType;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.chatbot.repository.ChatMessageRepository;
import com.wilo.server.chatbot.repository.ChatSessionRepository;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiChatClient aiChatClient;
    private final ObjectMapper objectMapper;

    public ChatMessageSendResponse sendMessage(
            Long sessionId,
            String guestIdHeader,
            ChatMessageSendRequest request
    ) {

        Long userId = extractUserIdIfAuthenticated();
        String guestId = normalizeGuestId(guestIdHeader);

        // 로그인도 아니고 guest도 아니면 불가
        if (userId == null && guestId == null) {
            throw new ApplicationException(ChatbotErrorCase.GUEST_ID_REQUIRED);
        }

        // 세션 조회
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() ->
                        new ApplicationException(ChatbotErrorCase.SESSION_NOT_FOUND));

        // 세션 소유자 검증
        boolean isOwner =
                (userId != null && session.getUserId() != null && session.getUserId().equals(userId))
                        || (userId == null && guestId != null && guestId.equals(session.getGuestId()));

        if (!isOwner) {
            throw new ApplicationException(ChatbotErrorCase.SESSION_FORBIDDEN);
        }

        // USER 메시지 저장
        MessageType type =
                request.getMessageType() == null
                        ? MessageType.TEXT
                        : request.getMessageType();

        ChatMessage savedUser =
                chatMessageRepository.save(
                        ChatMessage.createUser(
                                sessionId,
                                type,
                                request.getMessage()
                        )
                );

        // AI 서버 호출
        Long personaId = session.getChatbotType().getId();

        String requester = userId != null ? String.valueOf(userId) : guestId;

        AiChatResult aiResult =
                aiChatClient.chat(
                        AiChatCommand.builder()
                                .userId(requester)
                                .sessionId(sessionId)
                                .personaId(personaId)
                                .message(request.getMessage())
                                .build()
                );


        // AI 응답 검증
        if (aiResult == null || aiResult.getAnswer() == null) {
            throw new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID);
        }

        // choices JSON 변환
        String choicesJson = null;

        if (aiResult.getChoices() != null) {

            try {
                choicesJson = objectMapper.writeValueAsString(aiResult.getChoices());
            } catch (Exception e) {

                throw new ApplicationException(
                        ChatbotErrorCase.AI_RESPONSE_INVALID, e);
            }
        }

        // BOT 메시지 저장
        ChatMessage savedBot =
                chatMessageRepository.save(
                        ChatMessage.createBot(
                                sessionId,
                                aiResult.getAnswer(),
                                aiResult.getSafetyStatus(),
                                choicesJson
                        )
                );

        // lastMessageAt 업데이트
        session.updateLastMessageAt(savedBot.getCreatedAt());

        return ChatMessageSendResponse.builder()
                .sessionId(sessionId)
                .userMessage(toDto(savedUser))
                .botMessage(toDto(savedBot))
                .build();
    }

    private ChatMessageDto toDto(ChatMessage m) {

        List<String> choices = null;

        if (m.getChoicesJson() != null && !m.getChoicesJson().isBlank()) {
            try {
                choices = objectMapper.readValue(
                        m.getChoicesJson(),
                        new TypeReference<List<String>>() {}
                );
            } catch (Exception ignored) {
                choices = null;
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

    // guestId 정규화
    private String normalizeGuestId(String guestIdHeader) {

        if (guestIdHeader == null) {
            return null;
        }

        String trimmed = guestIdHeader.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long extractUserIdIfAuthenticated() {

        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }

        if (auth.getPrincipal() instanceof Long userId) {
            return userId;
        }

        return null;
    }
}
