package com.wilo.server.chatbot.repository;

import com.wilo.server.chatbot.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
        select m
        from ChatMessage m
        where m.sessionId = :sessionId
          and (:cursor is null or m.id < :cursor)
        order by m.id desc
    """)
    List<ChatMessage> findBySessionIdCursorDesc(
            @Param("sessionId") Long sessionId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
        select m
        from ChatMessage m
        where m.sessionId = :sessionId
        order by m.id desc
    """)
    List<ChatMessage> findRecentDesc(@Param("sessionId") Long sessionId, Pageable pageable);

    @Query("""
        select m
        from ChatMessage m
        where m.sessionId = :sessionId
        order by m.id asc
    """)
    List<ChatMessage> findOldestAsc(@Param("sessionId") Long sessionId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("""
        delete from ChatMessage m
        where m.sessionId = :sessionId
          and m.id < :keepFromId
    """)
    int deleteOlderThan(@Param("sessionId") Long sessionId, @Param("keepFromId") Long keepFromId);
}
