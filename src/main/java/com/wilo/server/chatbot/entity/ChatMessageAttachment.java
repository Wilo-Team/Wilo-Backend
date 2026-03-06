package com.wilo.server.chatbot.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chat_message_attachments",
        indexes = {
                @Index(name = "idx_attachment_message", columnList = "message_id"),
                @Index(name = "idx_attachment_media", columnList = "media_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "media_id", nullable = false)
    private Long mediaId;

    public static ChatMessageAttachment create(Long messageId, Long mediaId) {
        ChatMessageAttachment attachment = new ChatMessageAttachment();
        attachment.messageId = messageId;
        attachment.mediaId = mediaId;
        return attachment;
    }
}
