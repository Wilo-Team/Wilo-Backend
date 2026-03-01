package com.wilo.server.chatbot.entity;

import com.wilo.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Entity
@Table(name = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    private SenderType senderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    @Lob
    @Column(nullable = false)
    private String content;

    // 봇 메시지에만 적용 (SAFE/WARNING/CRITICAL)
    @Enumerated(EnumType.STRING)
    @Column(name = "safety_status", length = 20)
    private SafetyStatus safetyStatus;

    // 봇 메시지 choices 3개를 JSON 문자열로 저장
    @Lob
    @Column(name = "choices_json")
    private String choicesJson;

    private static String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content는 비어 있을 수 없습니다.");
        }
        return content;
    }

    public static ChatMessage createUser(Long sessionId, MessageType messageType, String content) {
        ChatMessage m = new ChatMessage();
        m.sessionId = sessionId;
        m.senderType = SenderType.USER;
        m.messageType = (messageType == null) ? MessageType.TEXT : messageType;
        m.content = requireContent(content);
        m.safetyStatus = null;
        m.choicesJson = null;
        return m;
    }

    public static ChatMessage createBot(Long sessionId, String content, SafetyStatus safetyStatus, String choicesJson) {
        ChatMessage m = new ChatMessage();
        m.sessionId = sessionId;
        m.senderType = SenderType.BOT;
        m.messageType = MessageType.TEXT;
        m.content = requireContent(content);
        m.safetyStatus = safetyStatus;
        m.choicesJson = choicesJson;
        return m;
    }
}
