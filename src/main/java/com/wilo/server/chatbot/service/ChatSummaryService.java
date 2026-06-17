package com.wilo.server.chatbot.service;

import com.wilo.server.chatbot.client.AiSummarizeClient;
import com.wilo.server.chatbot.client.AiSummarizeResult;
import com.wilo.server.chatbot.client.dto.AiRoleMessage;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSummaryService {
    private final AiSummarizeClient aiSummarizeClient;
    private final ChatSummaryTxService chatSummaryTxService;
    private final Set<Long> runningSessionIds = ConcurrentHashMap.newKeySet();

    // 메시지 수가 충분히 쌓였으면 요약 생성 + 오래된 raw 삭제
    @Async("chatSummaryExecutor")
    public void summarizeIfNeededAsync(Long sessionId) {
        if (sessionId == null || !runningSessionIds.add(sessionId)) {
            return;
        }

        try {
            if (!chatSummaryTxService.shouldSummarize(sessionId)) {
                return;
            }
            summarizeAndTrim(sessionId);
        } catch (Exception e) {
            log.warn("요약 후처리 실패 sessionId={}", sessionId, e);
        } finally {
            runningSessionIds.remove(sessionId);
        }
    }

    // 강제 요약 (세션 만료 시 호출)
    public void summarizeAndTrim(Long sessionId) {
        List<AiRoleMessage> toSummarize = chatSummaryTxService.loadSummarizeInput(sessionId);
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

        chatSummaryTxService.saveSummaryAndTrim(sessionId, summary, keyTopics);
    }
}
