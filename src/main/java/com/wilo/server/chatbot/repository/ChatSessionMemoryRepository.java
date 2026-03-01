package com.wilo.server.chatbot.repository;

import com.wilo.server.chatbot.entity.ChatSessionMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatSessionMemoryRepository extends JpaRepository<ChatSessionMemory, Long> {
    Optional<ChatSessionMemory> findBySessionId(Long sessionId);
    boolean existsBySessionId(Long sessionId);
}
