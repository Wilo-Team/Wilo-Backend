package com.wilo.server.chatbot.client;

import com.wilo.server.chatbot.client.dto.AiChatRequest;
import com.wilo.server.chatbot.client.dto.AiChatResponse;
import com.wilo.server.chatbot.entity.SafetyStatus;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RealAiChatClient implements AiChatClient {

    private final WebClient aiWebClient;

    @Override
    public AiChatResult chat(AiChatCommand command) {

        AiChatRequest req = AiChatRequest.builder()
                .request_id(command.getRequestId())
                .user_id(command.getUserId())
                .session_id(String.valueOf(command.getSessionId()))
                .persona_id(command.getPersonaId())
                .message(command.getMessage())
                .context(AiChatRequest.Context.builder()
                        .recent_messages(command.getRecentMessages())
                        .session_summary(command.getSessionSummary())
                        .memory(command.getMemory())
                        .build())
                .build();

        AiChatResponse res = aiWebClient.post()
                .uri("/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .onStatus(
                        s -> s.value() == 502 || s.value() == 503 || s.value() == 504,
                        r -> Mono.error(new ApplicationException(ChatbotErrorCase.AI_SERVER_FAILED))
                )
                .onStatus(
                        s -> s.is4xxClientError(),
                        r -> Mono.error(new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID))
                )
                .bodyToMono(AiChatResponse.class)
                .timeout(Duration.ofSeconds(35))
                .block();

        if (res == null) {
            throw new ApplicationException(ChatbotErrorCase.AI_SERVER_FAILED);
        }

        // AI 내부 status 기반 실패 처리
        if (res.getStatus() != null && !"success".equalsIgnoreCase(res.getStatus())) {
            throw new ApplicationException(ChatbotErrorCase.AI_SERVER_FAILED);
        }

        if (res.getAnswer() == null || res.getAnswer().isBlank()) {
            throw new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID);
        }

        List<String> choices = res.getChoices();
        if (choices == null) {
            choices = List.of();
        }

        SafetyStatus safety = AiSafetyMapper.toBackend(res.getSafety_status());

        return AiChatResult.builder()
                .sessionId(command.getSessionId())
                .answer(res.getAnswer())
                .choices(choices)
                .safetyStatus(safety)
                .build();
    }
}