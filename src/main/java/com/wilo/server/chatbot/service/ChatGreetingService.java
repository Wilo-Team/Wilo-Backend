package com.wilo.server.chatbot.service;

import com.wilo.server.chatbot.client.AiChatResult;
import com.wilo.server.chatbot.client.AiGreetingClient;
import com.wilo.server.chatbot.client.AiGreetingCommand;
import com.wilo.server.chatbot.dto.ChatGreetingResponse;
import com.wilo.server.chatbot.entity.ChatMessage;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.chatbot.repository.ChatMessageRepository;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatGreetingService {

    private final AiGreetingClient aiGreetingClient;
    private final ChatMessageTxService chatMessageTxService;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatGreetingResponse createOrGetGreeting(Long sessionId, String guestIdHeader) {

        Long userId = extractUserIdIfAuthenticated();
        String guestId = normalizeGuestId(guestIdHeader);

        if (userId == null && guestId == null) {
            throw new ApplicationException(ChatbotErrorCase.GUEST_ID_REQUIRED);
        }

        // 세션 접근 검증 + persona
        String personaCode = chatMessageTxService.getPersonaCodeWithAuthCheck(sessionId, userId, guestId);

        // 이미 메시지가 있으면 greeting 중복 생성하지 않고 첫 BOT 반환
        var existingMessages = chatMessageRepository.findBySessionIdCursorDesc(sessionId, null, PageRequest.of(0, 1));
        if (!existingMessages.isEmpty()) {
            ChatMessage existingFirstBot = chatMessageRepository
                    .findAllBySessionIdOrderByIdAsc(sessionId)
                    .stream()
                    .filter(m -> m.getSenderType().name().equals("BOT"))
                    .findFirst()
                    .orElse(existingMessages.get(0));

            return ChatGreetingResponse.builder()
                    .sessionId(sessionId)
                    .botMessage(chatMessageTxService.toDto(existingFirstBot))
                    .build();
        }

        String requester = (userId != null) ? String.valueOf(userId) : guestId;
        String requestId = java.util.UUID.randomUUID().toString();

        AiChatResult aiResult = aiGreetingClient.greeting(
                AiGreetingCommand.builder()
                        .requestId(requestId)
                        .userId(requester)
                        .sessionId(sessionId)
                        .personaId(personaCode)
                        .sessionSummary(chatMessageTxService.getSessionSummary(sessionId))
                        .recentMessages(java.util.List.of())
                        .memory(null)
                        .build()
        );

        if (aiResult == null || aiResult.getAnswer() == null) {
            throw new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID);
        }

        ChatMessage savedBot = chatMessageTxService.saveBotMessageWithSessionUpdate(sessionId, aiResult);

        return ChatGreetingResponse.builder()
                .sessionId(sessionId)
                .botMessage(chatMessageTxService.toDto(savedBot))
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
