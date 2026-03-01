package com.wilo.server.chatbot.client;

import com.wilo.server.chatbot.client.dto.AiRoleMessage;

import java.util.List;

public interface AiSummarizeClient {
    AiSummarizeResult summarize(List<AiRoleMessage> messages);
}
