package com.wilo.server.chatbot.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chat_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그인 사용자면 user_id 세팅
    @Column(name = "user_id")
    private Long userId;

    // 게스트면 guest_id 세팅
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
}
