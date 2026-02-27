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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
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

        if (userId == null && guestId == null) {
            throw new ApplicationException(ChatbotErrorCase.GUEST_ID_REQUIRED);
        }

        // 세션 조회
        ChatSession session = loadSessionWithAuthCheck(sessionId, userId, guestId);

        // USER 메시지 저장
        ChatMessage savedUser = saveUserMessage(sessionId, request);

        // AI 서버 호출 (트랜잭션 X)
        AiChatResult aiResult = callAi(session, userId, guestId, request);

        // BOT 메시지 저장
        ChatMessage savedBot = saveBotMessage(session, aiResult);

        return ChatMessageSendResponse.builder()
                .sessionId(sessionId)
                .userMessage(toDto(savedUser))
                .botMessage(toDto(savedBot))
                .build();
    }


    // 세션 조회 + 권한 체크
    @Transactional(readOnly = true)
    protected ChatSession loadSessionWithAuthCheck(
            Long sessionId,
            Long userId,
            String guestId
    ) {

        ChatSession session =
                chatSessionRepository.findById(sessionId)
                        .orElseThrow(() -> new ApplicationException(ChatbotErrorCase.SESSION_NOT_FOUND));

        boolean isOwner =
                (userId != null && session.getUserId() != null && session.getUserId().equals(userId))
                        || (userId == null && guestId != null && guestId.equals(session.getGuestId()));

        if (!isOwner) {
            throw new ApplicationException(ChatbotErrorCase.SESSION_FORBIDDEN);
        }

        return session;
    }


    // USER 메시지 저장
    @Transactional
    protected ChatMessage saveUserMessage(
            Long sessionId,
            ChatMessageSendRequest request
    ) {
        MessageType type =
                request.getMessageType() == null
                        ? MessageType.TEXT
                        : request.getMessageType();

        return chatMessageRepository.save(
                ChatMessage.createUser(
                        sessionId,
                        type,
                        request.getMessage()
                )
        );
    }


    // AI 서버 호출
    private AiChatResult callAi(
            ChatSession session,
            Long userId,
            String guestId,
            ChatMessageSendRequest request
    ) {
        Long personaId = session.getChatbotType().getId();

        String requester = userId != null ? String.valueOf(userId) : guestId;

        AiChatResult result =
                aiChatClient.chat(
                        AiChatCommand.builder()
                                .userId(requester)
                                .sessionId(session.getId())
                                .personaId(personaId)
                                .message(request.getMessage())
                                .build()
                );

        if (result == null || result.getAnswer() == null) {
            throw new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID);
        }
        return result;
    }


    // BOT 메시지 저장 + lastMessageAt 업데이트
    @Transactional
    protected ChatMessage saveBotMessage(
            ChatSession session,
            AiChatResult aiResult
    ) {
        String choicesJson = null;

        if (aiResult.getChoices() != null) {
            try {
                choicesJson = objectMapper.writeValueAsString(aiResult.getChoices());
            } catch (Exception e) {
                throw new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID, e);
            }
        }

        ChatMessage savedBot =
                chatMessageRepository.save(
                        ChatMessage.createBot(
                                session.getId(),
                                aiResult.getAnswer(),
                                aiResult.getSafetyStatus(),
                                choicesJson
                        )
                );

        session.updateLastMessageAt(savedBot.getCreatedAt());
        return savedBot;
    }


    // DTO 변환
    private ChatMessageDto toDto(ChatMessage m) {
        List<String> choices = null;

        if (m.getChoicesJson() != null
                && !m.getChoicesJson().isBlank()) {

            try {
                choices = objectMapper.readValue(m.getChoicesJson(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("choices parse fail id={}", m.getId());
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


    // 로그인 userId 추출
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