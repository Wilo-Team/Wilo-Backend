package com.wilo.server.chatbot.client;

import com.wilo.server.chatbot.client.dto.AiChatRequest;
import com.wilo.server.chatbot.client.dto.AiChatResponse;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealAiChatClient implements AiChatClient {

    private final WebClient aiWebClient;

    @Override
    public AiChatResult chat(AiChatCommand command) {

        AiChatRequest.Context context =
                AiChatRequest.Context.builder()
                        .recentMessages(command.getRecentMessages() == null ? List.of() : command.getRecentMessages())
                        .sessionSummary(command.getSessionSummary() == null ? "" : command.getSessionSummary())
                        .memory(command.getMemory() == null ? Map.of() : command.getMemory())
                        .build();


        AiChatRequest req = AiChatRequest.builder()
                .requestId(command.getRequestId())
                .userId(command.getUserId())
                .sessionId(String.valueOf(command.getSessionId()))
                .personaId(command.getPersonaId())
                .message(command.getMessage())
                .context(context)
                .build();


        AiChatResponse res = aiWebClient.post()
                .uri("/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .onStatus(s -> s.value() == 502 || s.value() == 503 || s.value() == 504,
                        r -> Mono.error(new ApplicationException(ChatbotErrorCase.AI_SERVER_FAILED))
                )
                .onStatus(
                        s -> s.is4xxClientError(),
                        r -> r.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.warn("AI chat 4xx from upstream. requestId={}, status={}", command.getRequestId(), r.statusCode());
                                    return Mono.error(new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID));
                                })
                )
                .bodyToMono(AiChatResponse.class)
                .timeout(Duration.ofSeconds(35))
                .block();


        if (res == null) {
            throw new ApplicationException(ChatbotErrorCase.AI_SERVER_FAILED);
        }

        if (!"success".equalsIgnoreCase(res.getStatus())) {
            throw new ApplicationException(ChatbotErrorCase.AI_SERVER_FAILED);
        }

        if (res.getAnswer() == null || res.getAnswer().isBlank()) {
            throw new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID);
        }


        return AiChatResult.builder()
                .sessionId(command.getSessionId())
                .answer(res.getAnswer())
                .choices(res.getChoices() == null ? List.of() : res.getChoices())
                .safetyStatus(AiSafetyMapper.toBackend(res.getSafetyStatus()))
                .build();
    }
}