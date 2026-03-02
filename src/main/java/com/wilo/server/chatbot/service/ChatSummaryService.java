package com.wilo.server.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilo.server.chatbot.client.AiSummarizeClient;
import com.wilo.server.chatbot.client.AiSummarizeResult;
import com.wilo.server.chatbot.client.dto.AiRoleMessage;
import com.wilo.server.chatbot.entity.ChatMessage;
import com.wilo.server.chatbot.entity.ChatSessionMemory;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.chatbot.repository.ChatMessageRepository;
import com.wilo.server.chatbot.repository.ChatSessionMemoryRepository;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSummaryService {
    private static final int KEEP_LAST_MESSAGES = 30;        // DB에 남길 최근 raw 개수
    private static final int SUMMARIZE_TRIGGER_COUNT = 40;   // raw가 이 이상이면 요약 돌림

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionMemoryRepository chatSessionMemoryRepository;
    private final AiSummarizeClient aiSummarizeClient;
    private final ObjectMapper objectMapper;

    // 메시지 수가 충분히 쌓였으면 요약 생성 + 오래된 raw 삭제
    @Transactional
    public void summarizeIfNeeded(Long sessionId) {
        List<Long> probe = chatMessageRepository.findRecentIds(
                sessionId, PageRequest.of(0, SUMMARIZE_TRIGGER_COUNT)
        );

        if (probe.size() < SUMMARIZE_TRIGGER_COUNT) {
            return;
        }

        summarizeAndTrim(sessionId);
    }

    // 강제 요약 (세션 만료 시 호출)
    @Transactional
    public void summarizeAndTrim(Long sessionId) {

        List<ChatMessage> all = chatMessageRepository.findAllForSummary(sessionId);
        if (all.isEmpty()) return;

        // AI로 보낼 messages 구성
        List<AiRoleMessage> toSummarize = buildSummarizeInput(sessionId, all);
        if (toSummarize.isEmpty()) return;

        // AI summarize 호출
        AiSummarizeResult result;
        try {
            result = aiSummarizeClient.summarize(toSummarize);
        } catch (Exception e) {
            log.error("summarize failed sessionId={}", sessionId, e);
            throw new ApplicationException(ChatbotErrorCase.AI_SERVER_FAILED, e);
        }

        if (result == null) {
            throw new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID);
        }

        String summary = (result.getSummary() == null) ? "" : result.getSummary().trim();
        List<String> keyTopics = (result.getKeyTopics() == null) ? List.of() : result.getKeyTopics();

        // summary 비어있으면 저장 스킵
        if (summary.isBlank()) {
            return;
        }

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

        // 오래된 raw 삭제 (최근 KEEP_LAST_MESSAGES만 유지)
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

        // 최근 KEEP_LAST_MESSAGES개의 가장 오래된 id를 구해서, 그보다 작은 것들을 전부 삭제
        List<Long> recentIds = chatMessageRepository.findRecentIds(
                sessionId, PageRequest.of(0, KEEP_LAST_MESSAGES)
        );

        if (recentIds.size() < KEEP_LAST_MESSAGES) {
            return;
        }

        Long keepFromId = recentIds.get(recentIds.size() - 1);

        // keepFromId 보다 작은 것들 삭제
        chatMessageRepository.deleteOlderThan(sessionId, keepFromId);
    }
}