package com.wilo.server.chatbot.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chat_session_memory")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSessionMemory {
    @Id
    @Column(name = "session_id")
    private Long sessionId;

    @Lob
    @Column(nullable = false)
    private String summary;

    @Lob
    @Column(name = "key_topics_json", nullable = false)
    private String keyTopicsJson;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ChatSessionMemory upsert(Long sessionId, String summary, String keyTopicsJson) {
        ChatSessionMemory m = new ChatSessionMemory();
        m.sessionId = sessionId;
        m.summary = (summary == null) ? "" : summary;
        m.keyTopicsJson = (keyTopicsJson == null) ? "[]" : keyTopicsJson;
        m.updatedAt = LocalDateTime.now();
        return m;
    }

    public void update(String summary, String keyTopicsJson) {
        this.summary = (summary == null) ? "" : summary;
        this.keyTopicsJson = (keyTopicsJson == null) ? "[]" : keyTopicsJson;
        this.updatedAt = LocalDateTime.now();
    }
}
