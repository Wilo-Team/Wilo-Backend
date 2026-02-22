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

    @Column(name = "sender_type", nullable = false, length = 20)
    private String senderType; // USER, BOT

    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType; // TEXT, IMAGE, VOICE

    @Lob
    @Column(nullable = false)
    private String content;

    // 봇 메시지에만 적용 (SAFE/WARNING/CRITICAL)
    @Column(name = "safety_status", length = 20)
    private String safetyStatus;

    // 봇 메시지 choices 3개를 JSON 문자열로 저장
    @Lob
    @Column(name = "choices_json")
    private String choicesJson;
}
