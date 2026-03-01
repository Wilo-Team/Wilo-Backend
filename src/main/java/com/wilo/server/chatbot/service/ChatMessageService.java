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

        // 세션 권한 체크 + personaId 얻기
        Long personaId = chatMessageTxService.getPersonaIdWithAuthCheck(sessionId, userId, guestId);

        // USER 메시지 저장
        ChatMessage savedUser = chatMessageTxService.saveUserMessage(sessionId, request);

        // AI 호출
        String requester = (userId != null) ? String.valueOf(userId) : guestId;

        AiChatResult aiResult = aiChatClient.chat(
                AiChatCommand.builder()
                        .userId(requester)
                        .sessionId(sessionId)
                        .personaId(personaId)
                        .message(request.getMessage())
                        .build()
        );

        if (aiResult == null || aiResult.getAnswer() == null) {
            throw new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID);
        }

        // BOT 메시지 저장 + lastMessageAt 업데이트
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