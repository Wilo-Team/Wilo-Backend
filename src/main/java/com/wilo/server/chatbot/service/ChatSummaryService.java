package com.wilo.server.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilo.server.chatbot.client.AiSummarizeClient;
import com.wilo.server.chatbot.client.AiSummarizeResult;
import com.wilo.server.chatbot.client.dto.AiRoleMessage;
import com.wilo.server.chatbot.entity.ChatMessage;
import com.wilo.server.chatbot.entity.ChatSessionMemory;
import com.wilo.server.chatbot.repository.ChatMessageRepository;
import com.wilo.server.chatbot.repository.ChatSessionMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSummaryService {

    private static final int KEEP_RECENT_N = 30;     // 최근 N턴 유지
    private static final int SUMMARIZE_BATCH = 1000; // 요약할 최대 raw 수

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionMemoryRepository chatSessionMemoryRepository;
    private final AiSummarizeClient aiSummarizeClient;
    private final ObjectMapper objectMapper;

    // 세션 만료 시 호출
    @Transactional
    public void summarizeAndPrune(Long sessionId) {

        // 최신 KEEP_RECENT_N개를 남기기 위해, 최신 N개 중 가장 작은 id를 keepFromId로 사용
        List<ChatMessage> recentDesc = chatMessageRepository.findRecentDesc(
                sessionId,
                PageRequest.of(0, KEEP_RECENT_N)
        );

        if (recentDesc.isEmpty()) {
            return;
        }

        Long keepFromId = recentDesc.stream()
                .map(ChatMessage::getId)
                .min(Long::compareTo)
                .orElse(null);

        // keepFromId 이전의 오래된 메시지들을 요약 대상으로 수집 (DB에 오래된 메시지가 많이 쌓이지 않도록 상한 설정)
        List<ChatMessage> oldestAsc = chatMessageRepository.findOldestAsc(sessionId, PageRequest.of(0, SUMMARIZE_BATCH));

        // 전체가 KEEP_RECENT_N 이하라면 prune/요약 스킵
        if (oldestAsc.size() <= KEEP_RECENT_N) {
            return;
        }

        // 요약 대상 = keepFromId 미만
        List<ChatMessage> toSummarize = oldestAsc.stream()
                .filter(m -> m.getId() < keepFromId)
                .toList();

        if (toSummarize.isEmpty()) {
            return;
        }

        // role+content만 전달
        List<AiRoleMessage> aiMsgs = toSummarize.stream()
                .map(m -> AiRoleMessage.builder()
                        .role(m.getSenderType().name().equals("USER") ? "user" : "assistant")
                        .content(m.getContent()) // TODO: PII sanitizer 적용
                        .build())
                .toList();

        AiSummarizeResult result = aiSummarizeClient.summarize(aiMsgs);

        // 빈 입력이면 summary="" 내려올 수 있음. 빈 요약이면 저장 스킵
        if (result.getSummary() == null || result.getSummary().isBlank()) {
            log.info("summarize empty sessionId={}", sessionId);
            return;
        }

        String keyTopicsJson;
        try {
            keyTopicsJson = objectMapper.writeValueAsString(result.getKeyTopics());
        } catch (Exception e) {
            keyTopicsJson = "[]";
        }

        ChatSessionMemory mem = chatSessionMemoryRepository.findById(sessionId).orElse(null);
        if (mem == null) {
            chatSessionMemoryRepository.save(
                    ChatSessionMemory.upsert(sessionId, result.getSummary(), keyTopicsJson)
            );
        } else {
            mem.update(result.getSummary(), keyTopicsJson);
        }

        // 오래된 raw 삭제
        int deleted = chatMessageRepository.deleteOlderThan(sessionId, keepFromId);
        log.info("prune sessionId={} keepFromId={} deleted={}", sessionId, keepFromId, deleted);
    }
}
