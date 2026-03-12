package com.wilo.server.chatbot.repository;

import com.wilo.server.chatbot.entity.ChatSession;
import com.wilo.server.chatbot.entity.ChatSessionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    @Query("""
        select cs
        from ChatSession cs
            left join fetch cs.chatbotType
        where
            (
                (:userId is not null and cs.userId = :userId)
                or
                (:guestId is not null and cs.guestId = :guestId)
            )
          and cs.status = :status
          and (
                :cursorActivityAt is null
                or (
                    coalesce(cs.lastMessageAt, cs.createdAt) < :cursorActivityAt
                    or (
                        coalesce(cs.lastMessageAt, cs.createdAt) = :cursorActivityAt
                        and cs.id < :cursorId
                    )
                )
          )
        order by coalesce(cs.lastMessageAt, cs.createdAt) desc, cs.id desc
    """)
    List<ChatSession> findSessions(
            @Param("userId") Long userId,
            @Param("guestId") String guestId,
            @Param("status") ChatSessionStatus status,
            @Param("cursorActivityAt") LocalDateTime cursorActivityAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
        select cs
        from ChatSession cs
          join fetch cs.chatbotType
        where cs.id = :sessionId
    """)
    Optional<ChatSession> findByIdWithChatbotType(
            @Param("sessionId") Long sessionId
    );

    @Query("""
        select s.id
        from ChatSession s
        where s.id in :ids
          and (
            (:userId is not null and s.userId = :userId)
            or (:userId is null and :guestId is not null and s.guestId = :guestId)
          )
    """)
    List<Long> findOwnedSessionIds(
            @Param("ids") Collection<Long> ids,
            @Param("userId") Long userId,
            @Param("guestId") String guestId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from ChatSession s
        where s.id in :ids
    """)
    int deleteByIds(@Param("ids") Collection<Long> ids);
}