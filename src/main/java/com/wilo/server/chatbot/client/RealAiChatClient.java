package com.wilo.server.chatbot.client;

import com.wilo.server.chatbot.exception.ChatbotErrorCase;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RealAiChatClient implements AiChatClient {

    private final WebClient aiWebClient;

    @Override
    public AiChatResult chat(AiChatCommand command) {
        try {
            Map<String, Object> body = Map.of(
                    "user_id", command.getUserId(),
                    "session_id", command.getSessionId(),
                    "persona_id", command.getPersonaId(),
                    "message", command.getMessage()
            );

            // AI 서버 응답도 Map으로 먼저 받고, 백엔드에서 파싱
            Map<String, Object> res = aiWebClient.post()
                    .uri("/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (res == null) {
                throw new ApplicationException(ChatbotErrorCase.AI_SERVER_FAILED);
            }

            return AiResponseMapper.toResult(res);

        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException(ChatbotErrorCase.AI_SERVER_FAILED, e);
        }
    }
}
