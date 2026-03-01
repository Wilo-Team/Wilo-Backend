package com.wilo.server.chatbot.client;

public interface AiChatClient {
    AiChatResult chat(AiChatCommand command);
}
