package com.wilo.server.chatbot.service;

import com.wilo.server.chatbot.dto.ChatSessionDeleteRequest;
import com.wilo.server.chatbot.dto.ChatSessionDeleteResponse;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.chatbot.repository.ChatMessageRepository;
import com.wilo.server.chatbot.repository.ChatSessionMemoryRepository;
import com.wilo.server.chatbot.repository.ChatSessionRepository;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatSessionDeleteService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionMemoryRepository chatSessionMemoryRepository;

    @Transactional
    public ChatSessionDeleteResponse deleteSessions(String guestIdHeader, ChatSessionDeleteRequest request) {

        Long userId = extractUserIdIfAuthenticated();
        String guestId = normalizeGuestId(guestIdHeader);

        if (userId == null && guestId == null) {
            // 게스트 요청이면 guestId 필수
            throw new ApplicationException(ChatbotErrorCase.GUEST_ID_REQUIRED);
        }

        List<Long> sessionIds = request.getSessionIds();
        if (sessionIds == null || sessionIds.isEmpty()) {
            throw new ApplicationException(ChatbotErrorCase.SESSION_DELETE_INVALID);
        }

        var uniqueIds = new HashSet<Long>();
        for (Long id : sessionIds) {
            if (id == null || id <= 0) {
                throw new ApplicationException(ChatbotErrorCase.SESSION_DELETE_INVALID);
            }
            uniqueIds.add(id);
        }

        // 소유권 검증
        List<Long> ownedIds = chatSessionRepository.findOwnedSessionIds(uniqueIds, userId, guestId);
        if (ownedIds.size() != uniqueIds.size()) {
            throw new ApplicationException(ChatbotErrorCase.SESSION_DELETE_INVALID);
        }

        chatMessageRepository.deleteBySessionIds(uniqueIds);
        chatSessionMemoryRepository.deleteBySessionIds(uniqueIds);

        // 세션 삭제
        int deletedCount = chatSessionRepository.deleteByIds(uniqueIds);

        return ChatSessionDeleteResponse.of(deletedCount);
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
