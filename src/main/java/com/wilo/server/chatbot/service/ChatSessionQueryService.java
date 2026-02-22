package com.wilo.server.chatbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilo.server.chatbot.dto.*;
import com.wilo.server.chatbot.entity.ChatMessage;
import com.wilo.server.chatbot.entity.ChatSession;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.chatbot.repository.ChatMessageRepository;
import com.wilo.server.chatbot.repository.ChatSessionRepository;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatSessionQueryService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public ChatSessionDetailResponse getSessionDetail(
            Long sessionId,
            String guestIdHeader,
            Long cursor,
            Integer size
    ) {
        Long userId = extractUserIdIfAuthenticated();
        String guestId = normalizeGuestId(guestIdHeader);

        if (userId == null && guestId == null) {
            throw new ApplicationException(ChatbotErrorCase.GUEST_ID_REQUIRED);
        }

        // 파라미터 검증
        int pageSize = (size == null) ? 30 : size;
        if (pageSize < 1 || pageSize > 100) {
            throw new ApplicationException(ChatbotErrorCase.INVALID_PARAMETER);
        }

        if (cursor != null && cursor <= 0) {
            throw new ApplicationException(ChatbotErrorCase.INVALID_PARAMETER);
        }

        // 세션 조회
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApplicationException(ChatbotErrorCase.SESSION_NOT_FOUND));

        // userId 또는 guestId 소유자만
        boolean isOwner =
                (userId != null && session.getUserId() != null && session.getUserId().equals(userId))
                        || (userId == null && guestId != null && guestId.equals(session.getGuestId()));

        if (!isOwner) {
            throw new ApplicationException(ChatbotErrorCase.SESSION_FORBIDDEN);
        }

        // 메시지 조회 => id DESC 커서 페이징
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        List<ChatMessage> messagesDesc = chatMessageRepository.findBySessionIdCursorDesc(
                sessionId,
                cursor,
                pageable
        );

        boolean hasNext = messagesDesc.size() > pageSize;
        if (hasNext) {
            messagesDesc = messagesDesc.subList(0, pageSize);
        }

        // 응답은 오래된 → 최신
        Collections.reverse(messagesDesc);

        List<ChatMessageDto> messageDtos = messagesDesc.stream()
                .map(this::toMessageDto)
                .toList();

        Long nextCursor = hasNext ? messagesDesc.get(0).getId() : null;

        Long safeNextCursor = hasNext
                ? messagesDesc.stream().map(ChatMessage::getId).min(Long::compareTo).orElse(null)
                : null;

        return ChatSessionDetailResponse.builder()
                .session(toSessionSummary(session))
                .messages(messageDtos)
                .summary(ChatSessionMemorySummaryDto.builder()
                        .exists(false)
                        .text(null)
                        .updatedAt(null)
                        .build())
                .nextCursor(safeNextCursor)
                .hasNext(hasNext)
                .build();
    }

    private ChatSessionSummaryDto toSessionSummary(ChatSession session) {
        return ChatSessionSummaryDto.builder()
                .sessionId(session.getId())
                .title(session.getTitle())
                .status(session.getStatus().name())
                .chatbotType(ChatbotTypeDto.builder()
                        .id(session.getChatbotType().getId())
                        .name(session.getChatbotType().getName())
                        .build())
                .build();
    }

    private ChatMessageDto toMessageDto(ChatMessage m) {
        List<String> choices = null;

        if (m.getChoicesJson() != null && !m.getChoicesJson().isBlank()) {
            try {
                choices = objectMapper.readValue(m.getChoicesJson(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
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
                .attachments(List.of()) // attachment 테이블 붙이면 조회해서 채우기
                .build();
    }

    private String normalizeGuestId(String guestIdHeader) {
        if (guestIdHeader == null) return null;
        String trimmed = guestIdHeader.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long extractUserIdIfAuthenticated() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        if (auth.getPrincipal() instanceof Long userId) return userId;
        return null;
    }
}
