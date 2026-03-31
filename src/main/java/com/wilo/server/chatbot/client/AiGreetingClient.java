package com.wilo.server.chatbot.client;

public interface AiGreetingClient {
    AiChatResult greeting(AiGreetingCommand command);
}