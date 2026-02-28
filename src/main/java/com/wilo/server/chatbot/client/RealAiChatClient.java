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
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RealAiChatClient implements AiChatClient {

    private final WebClient aiWebClient;

    @Override
    public AiChatResult chat(AiChatCommand command) {

        AiChatRequest.Context context =
                AiChatRequest.Context.builder()
                        .recent_messages(command.getRecentMessages() == null ? List.of() : command.getRecentMessages())
                        .session_summary(command.getSessionSummary() == null ? "" : command.getSessionSummary())
                        .memory(command.getMemory() == null ? Map.of() : command.getMemory())
                        .build();


        AiChatRequest req = AiChatRequest.builder()
                .request_id(command.getRequestId())
                .user_id(command.getUserId())
                .session_id(String.valueOf(command.getSessionId()))
                .persona_id(command.getPersonaId())
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
                                    System.out.println("AI ERROR BODY = " + body);
                                    return Mono.error(
                                            new ApplicationException(
                                                    ChatbotErrorCase.AI_RESPONSE_INVALID
                                            )
                                    );
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
                .safetyStatus(AiSafetyMapper.toBackend(res.getSafety_status()))
                .build();
    }
}