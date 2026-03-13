package com.wilo.server.chatbot.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chatbot_types")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatbotType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "background_color", length = 20)
    private String backgroundColor;

    @Column(name = "border_color", length = 20)
    private String borderColor;

    public static ChatbotType create(
            String code,
            String name,
            String description,
            String imageUrl,
            boolean isActive,
            String backgroundColor,
            String borderColor
    ) {
        ChatbotType t = new ChatbotType();
        t.code = code;
        t.name = name;
        t.description = description;
        t.imageUrl = imageUrl;
        t.isActive = isActive;
        t.backgroundColor = backgroundColor;
        t.borderColor = borderColor;
        return t;
    }

    public void update(
            String name,
            String description,
            String imageUrl,
            String backgroundColor,
            String borderColor
    ) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
    }
}
