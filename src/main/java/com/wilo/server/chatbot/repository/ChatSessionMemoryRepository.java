package com.wilo.server.chatbot.repository;

import com.wilo.server.chatbot.entity.ChatSessionMemory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionMemoryRepository extends JpaRepository<ChatSessionMemory, Long> {
}
