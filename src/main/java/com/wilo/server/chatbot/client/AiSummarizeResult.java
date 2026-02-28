package com.wilo.server.chatbot.client;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiSummarizeResult {
    private String summary;
    private List<String> keyTopics;
}
