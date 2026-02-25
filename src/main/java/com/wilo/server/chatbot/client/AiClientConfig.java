package com.wilo.server.chatbot.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;


@Configuration
public class AiClientConfig {
    @Bean
    public WebClient aiWebClient(@Value("${ai.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
