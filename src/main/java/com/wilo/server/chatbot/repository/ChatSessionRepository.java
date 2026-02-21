package com.wilo.server.chatbot.repository;

import com.wilo.server.chatbot.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
}
