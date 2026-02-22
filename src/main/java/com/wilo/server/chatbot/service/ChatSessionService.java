package com.wilo.server.chatbot.service;

import com.wilo.server.chatbot.dto.*;
import com.wilo.server.chatbot.entity.ChatSession;
import com.wilo.server.chatbot.entity.ChatSessionStatus;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.chatbot.repository.ChatSessionRepository;
import com.wilo.server.chatbot.repository.ChatbotTypeRepository;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatbotTypeRepository chatbotTypeRepository;

    public ChatSessionCreateResponse createSession(ChatSessionCreateRequest request, String guestIdHeader) {

        Long userId = extractUserIdIfAuthenticated();

        var chatbotType = chatbotTypeRepository.findById(request.getChatbotTypeId())
                .filter(type -> type.isActive())
                .orElseThrow(() -> new ApplicationException(ChatbotErrorCase.CHATBOT_TYPE_NOT_FOUND));

        ChatSession session;

        if (userId != null) {
            session = ChatSession.createForUser(userId, chatbotType);
        } else {
            String guestId = normalizeGuestId(guestIdHeader);
            if (guestId == null) {
                throw new ApplicationException(ChatbotErrorCase.GUEST_ID_REQUIRED);
            }
            session = ChatSession.createForGuest(guestId, chatbotType);
        }

        ChatSession saved = chatSessionRepository.save(session);
        return ChatSessionCreateResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ChatSessionListResponse getSessions(
            String guestIdHeader,
            ChatSessionListRequest request
    ) {
        Long userId = extractUserIdIfAuthenticated();
        String guestId = normalizeGuestId(guestIdHeader);

        // 비로그인인데 guestId 없으면 불가
        if (userId == null && guestId == null) {
            throw new ApplicationException(ChatbotErrorCase.GUEST_ID_REQUIRED);
        }

        // status default = ACTIVE
        ChatSessionStatus status = ChatSessionStatus.ACTIVE;
        if (request.status() != null && !request.status().isBlank()) {
            try {
                status = ChatSessionStatus.valueOf(request.status());
            } catch (IllegalArgumentException e) {
                throw new ApplicationException(ChatbotErrorCase.INVALID_PARAMETER);
            }
        }

        if (status == ChatSessionStatus.DELETED) {
            throw new ApplicationException(ChatbotErrorCase.INVALID_PARAMETER);
        }

        // size default=20, max=50
        int size = (request.size() == null) ? 20 : request.size();
        if (size < 1 || size > 50) {
            throw new ApplicationException(ChatbotErrorCase.INVALID_PARAMETER);
        }

        Long cursor = request.cursor();

        if (cursor != null && cursor <= 0) {
            throw new ApplicationException(ChatbotErrorCase.INVALID_PARAMETER);
        }

        Pageable pageable = PageRequest.of(0, size + 1);

        List<ChatSession> sessions = chatSessionRepository.findSessions(
                userId,
                guestId,
                status,
                cursor,
                pageable
        );

        boolean hasNext = sessions.size() > size;
        if (hasNext) {
            sessions = sessions.subList(0, size);
        }

        List<ChatSessionListItem> items = sessions.stream()
                .map(this::toItem)
                .toList();

        Long nextCursor = hasNext ? sessions.get(sessions.size() - 1).getId() : null;

        return ChatSessionListResponse.builder()
                .sessions(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    private ChatSessionListItem toItem(ChatSession session) {
        String preview = session.getTitle();

        return ChatSessionListItem.builder()
                .sessionId(session.getId())
                .title(session.getTitle())
                .chatbotType(ChatbotTypeDto.builder()
                        .id(session.getChatbotType().getId())
                        .name(session.getChatbotType().getName())
                        .build())
                .status(session.getStatus().name())
                .lastMessageAt(session.getLastMessageAt())
                .preview(preview)
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
