package com.wilo.server.chatbot.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chat_suggestion_presets",
        indexes = {
                @Index(name = "idx_preset_type_active_order", columnList = "chatbot_type_id,is_active,sort_order")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSuggestionPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatbot_type_id", nullable = false)
    private ChatbotType chatbotType;

    @Column(nullable = false, length = 200)
    private String text;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    public static ChatSuggestionPreset create(ChatbotType type, String text, int sortOrder, boolean active) {
        ChatSuggestionPreset p = new ChatSuggestionPreset();
        p.chatbotType = type;
        p.text = text;
        p.sortOrder = sortOrder;
        p.isActive = active;
        return p;
    }
}
