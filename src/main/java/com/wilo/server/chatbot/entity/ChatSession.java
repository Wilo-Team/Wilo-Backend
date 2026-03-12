package com.wilo.server.chatbot.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "chat_sessions",
        indexes = {
                @Index(name = "idx_chat_sessions_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "guest_id", length = 100)
    private String guestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatbot_type_id", nullable = false)
    private ChatbotType chatbotType;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatSessionStatus status;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public static ChatSession createForUser(Long userId, ChatbotType chatbotType) {
        ChatSession session = new ChatSession();
        session.userId = userId;
        session.guestId = null;
        session.chatbotType = chatbotType;
        session.title = "새로운 대화";
        session.status = ChatSessionStatus.ACTIVE;
        session.lastMessageAt = null;
        return session;
    }

    public static ChatSession createForGuest(String guestId, ChatbotType chatbotType) {
        ChatSession session = new ChatSession();
        session.userId = null;
        session.guestId = guestId;
        session.chatbotType = chatbotType;
        session.title = "새로운 대화";
        session.status = ChatSessionStatus.ACTIVE;
        session.lastMessageAt = null;
        return session;
    }

    public void updateLastMessageAt(LocalDateTime time) {
        this.lastMessageAt = time;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void archive() {
        this.status = ChatSessionStatus.ARCHIVED;
    }

    public void restore() {
        this.status = ChatSessionStatus.ACTIVE;
    }
}