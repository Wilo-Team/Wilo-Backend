package com.wilo.server.chatbot.repository;

import com.wilo.server.chatbot.entity.ChatSessionMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface ChatSessionMemoryRepository extends JpaRepository<ChatSessionMemory, Long> {
    Optional<ChatSessionMemory> findBySessionId(Long sessionId);
    boolean existsBySessionId(Long sessionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from ChatSessionMemory m
        where m.sessionId in :sessionIds
    """)
    int deleteBySessionIds(@Param("sessionIds") Collection<Long> sessionIds);
}
