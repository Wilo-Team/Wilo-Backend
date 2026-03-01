package com.wilo.server.chatbot.client;

import com.wilo.server.chatbot.client.dto.AiRoleMessage;
import com.wilo.server.chatbot.client.dto.AiSummarizeRequest;
import com.wilo.server.chatbot.client.dto.AiSummarizeResponse;
import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RealAiSummarizeClient implements AiSummarizeClient {

    private final WebClient aiWebClient;

    @Override
    public AiSummarizeResult summarize(List<AiRoleMessage> messages) {

        AiSummarizeRequest req = AiSummarizeRequest.builder()
                .messages(messages == null ? List.of() : messages)
                .build();

        AiSummarizeResponse res = aiWebClient.post()
                .uri("/summarize")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError,
                        r -> Mono.error(new ApplicationException(ChatbotErrorCase.AI_SERVER_FAILED)))
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new ApplicationException(ChatbotErrorCase.AI_RESPONSE_INVALID)))
                .bodyToMono(AiSummarizeResponse.class)
                .timeout(Duration.ofSeconds(60))
                .retryWhen(
                        Retry.max(1).filter(ex -> ex instanceof java.util.concurrent.TimeoutException
                                        || ex instanceof ApplicationException).transientErrors(true)
                )
                .onErrorMap(java.util.concurrent.TimeoutException.class,
                        ex -> new ApplicationException(ChatbotErrorCase.AI_SERVER_FAILED))
                .block();

        if (res == null) {
            throw new ApplicationException(ChatbotErrorCase.AI_SERVER_FAILED);
        }

        return AiSummarizeResult.builder()
                .summary(res.getSummary() == null ? "" : res.getSummary())
                .keyTopics(res.getKey_topics() == null ? List.of() : res.getKey_topics())
                .build();
    }
}
