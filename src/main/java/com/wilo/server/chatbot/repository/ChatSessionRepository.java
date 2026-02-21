package com.wilo.server.chatbot.repository;

import com.wilo.server.chatbot.entity.ChatSession;
import com.wilo.server.chatbot.entity.ChatSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    @Query("""
        select cs
        from ChatSession cs
        where
            (
                (:userId is not null and cs.userId = :userId)
                or
                (:guestId is not null and cs.guestId = :guestId)
            )
        and cs.status = :status
        and (
            :cursor is null
            or cs.id < :cursor
        )
        order by cs.lastMessageAt desc nulls last, cs.id desc
    """)
    List<ChatSession> findSessions(
            @Param("userId") Long userId,
            @Param("guestId") String guestId,
            @Param("status") ChatSessionStatus status,
            @Param("cursor") Long cursor,
            org.springframework.data.domain.Pageable pageable
    );
}
