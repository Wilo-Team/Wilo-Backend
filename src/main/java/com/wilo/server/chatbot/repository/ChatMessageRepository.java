package com.wilo.server.chatbot.repository;

import com.wilo.server.chatbot.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 메시지 목록 조회 (커서 기반 페이징)
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


    // 최근 메시지 N개 조회 (AI chat용)
    @Query("""
        select m
        from ChatMessage m
        where m.sessionId = :sessionId
        order by m.id desc
    """)
    List<ChatMessage> findRecentDesc(
            @Param("sessionId") Long sessionId,
            Pageable pageable
    );


    // 요약 생성용 전체 메시지 조회
    @Query("""
        select m
        from ChatMessage m
        where m.sessionId = :sessionId
        order by m.id asc
    """)
    List<ChatMessage> findAllForSummary(
            @Param("sessionId") Long sessionId
    );

    // 유지할 메시지 기준 ID 조회
    @Query("""
        select m.id
        from ChatMessage m
        where m.sessionId = :sessionId
        order by m.id desc
    """)
    List<Long> findRecentIds(
            @Param("sessionId") Long sessionId,
            Pageable pageable
    );

    // 오래된 메시지 삭제
    @Modifying(clearAutomatically = true)
    @Query("""
        delete from ChatMessage m
        where m.sessionId = :sessionId
          and m.id < :keepFromId
    """)
    int deleteOlderThan(
            @Param("sessionId") Long sessionId,
            @Param("keepFromId") Long keepFromId
    );

    // 세션 ID 목록으로 메시지 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from ChatMessage m
        where m.sessionId in :sessionIds
    """)
    int deleteBySessionIds(@Param("sessionIds") Collection<Long> sessionIds);

    Optional<ChatMessage> findById(Long id);
}
