package com.wilo.server.chatbot.service;

import com.wilo.server.chatbot.client.AiChatClient;
import com.wilo.server.chatbot.client.AiChatCommand;
import com.wilo.server.chatbot.client.AiChatResult;
import com.wilo.server.chatbot.dto.ChatMessageSendRequest;
import com.wilo.server.chatbot.dto.ChatMessageSendResponse;
import com.wilo.server.chatbot.entity.ChatMessage;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final AiChatClient aiChatClient;
    private final ChatMessageTxService chatMessageTxService;

    public ChatMessageSendResponse sendMessage(Long sessionId, String guestIdHeader, ChatMessageSendRequest request) {

        Long userId = extractUserIdIfAuthenticated();
        String guestId = normalizeGuestId(guestIdHeader);

        if (userId == null && guestId == null) {
            throw new ApplicationException(ChatbotErrorCase.GUEST_ID_REQUIRED);
        }

        // personaCode 얻기
        String personaCode = chatMessageTxService.getPersonaCodeWithAuthCheck(sessionId, userId, guestId);

        // USER 저장
        ChatMessage savedUser = chatMessageTxService.saveUserMessage(sessionId, request);

        // context 구성 (최근 N턴 + summary)
        String summary = chatMessageTxService.getSessionSummary(sessionId);
        var recent = chatMessageTxService.findRecentAiMessages(sessionId, 20); // 정책값

        String requester = (userId != null) ? String.valueOf(userId) : guestId;
        String requestId = java.util.UUID.randomUUID().toString();

        AiChatResult aiResult = aiChatClient.chat(
                AiChatCommand.builder()
                        .requestId(requestId)
                        .userId(requester)
                        .sessionId(sessionId)
                        .personaId(personaCode)
                        .message(request.getMessage())
                        .sessionSummary(summary)
                        .recentMessages(recent)
                        .memory(null) // 1차는 null로
                        .build()
        );

        if (aiResult == null || aiResult.getAnswer() == null) {
            throw new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID);
        }

        ChatMessage savedBot = chatMessageTxService.saveBotMessageWithSessionUpdate(sessionId, aiResult);

        return ChatMessageSendResponse.builder()
                .sessionId(sessionId)
                .userMessage(chatMessageTxService.toDto(savedUser))
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