package com.wilo.server.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilo.server.chatbot.client.dto.AiRoleMessage;
import com.wilo.server.chatbot.entity.ChatMessage;
import com.wilo.server.chatbot.entity.ChatSessionMemory;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.chatbot.repository.ChatMessageRepository;
import com.wilo.server.chatbot.repository.ChatSessionMemoryRepository;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatSummaryTxService {
    private static final int KEEP_LAST_MESSAGES = 30;
    private static final int SUMMARIZE_TRIGGER_COUNT = 40;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionMemoryRepository chatSessionMemoryRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public boolean shouldSummarize(Long sessionId) {
        List<Long> probe = chatMessageRepository.findRecentIds(
                sessionId, PageRequest.of(0, SUMMARIZE_TRIGGER_COUNT)
        );

        return probe.size() >= SUMMARIZE_TRIGGER_COUNT;
    }

    @Transactional(readOnly = true)
    public List<AiRoleMessage> loadSummarizeInput(Long sessionId) {
        List<ChatMessage> all = chatMessageRepository.findAllForSummary(sessionId);
        if (all.isEmpty()) return List.of();

        return buildSummarizeInput(sessionId, all);
    }

    @Transactional
    public void saveSummaryAndTrim(Long sessionId, String summary, List<String> keyTopics) {
        String keyTopicsJson;
        try {
            keyTopicsJson = objectMapper.writeValueAsString(keyTopics);
        } catch (Exception e) {
            throw new ApplicationException(ChatbotErrorCase.CHATBOT_INTERNAL_SERVER_ERROR, e);
        }

        ChatSessionMemory memory = chatSessionMemoryRepository.findById(sessionId)
                .orElseGet(() -> ChatSessionMemory.upsert(sessionId, "", "[]"));

        memory.update(summary, keyTopicsJson);
        chatSessionMemoryRepository.save(memory);

        trimOldMessages(sessionId);
    }

    private List<AiRoleMessage> buildSummarizeInput(Long sessionId, List<ChatMessage> allAsc) {
        String previousSummary = chatSessionMemoryRepository.findById(sessionId)
                .map(ChatSessionMemory::getSummary)
                .orElse("");

        List<AiRoleMessage> out = new ArrayList<>();

        if (previousSummary != null && !previousSummary.isBlank()) {
            out.add(AiRoleMessage.builder()
                    .role("system")
                    .content("이전 세션 요약: " + previousSummary)
                    .build());
        }

        for (ChatMessage m : allAsc) {
            if (m.getContent() == null || m.getContent().isBlank()) continue;

            String role = (m.getSenderType().name().equals("USER")) ? "user" : "assistant";

            out.add(AiRoleMessage.builder()
                    .role(role)
                    .content(m.getContent())
                    .build());
        }

        return out;
    }

    private void trimOldMessages(Long sessionId) {
        List<Long> recentIds = chatMessageRepository.findRecentIds(
                sessionId, PageRequest.of(0, KEEP_LAST_MESSAGES)
        );

        if (recentIds.size() < KEEP_LAST_MESSAGES) {
            return;
        }

        Long keepFromId = recentIds.get(recentIds.size() - 1);
        chatMessageRepository.deleteOlderThan(sessionId, keepFromId);
    }
}
