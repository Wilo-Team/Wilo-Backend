package com.wilo.server.chatbot.service;

import com.wilo.server.chatbot.dto.ChatSessionCreateRequest;
import com.wilo.server.chatbot.dto.ChatSessionCreateResponse;
import com.wilo.server.chatbot.entity.ChatSession;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.chatbot.repository.ChatSessionRepository;
import com.wilo.server.chatbot.repository.ChatbotTypeRepository;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
