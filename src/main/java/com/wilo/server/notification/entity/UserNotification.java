package com.wilo.server.notification.entity;

import com.wilo.server.global.entity.BaseEntity;
import com.wilo.server.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "user_notifications",
        indexes = {
                @Index(name = "idx_notification_receiver_created_id", columnList = "receiver_user_id, created_at, id"),
                @Index(name = "idx_notification_receiver_isread_created_id", columnList = "receiver_user_id, is_read, created_at, id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private User receiverUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private User actorUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false)
    private Long postId;

    @Column
    private Long commentId;

    @Column(length = 300)
    private String commentPreview;

    @Column(nullable = false)
    private boolean isRead;

    @Column
    private LocalDateTime readAt;

    @Builder
    private UserNotification(
            User receiverUser,
            User actorUser,
            NotificationType type,
            Long postId,
            Long commentId,
            String commentPreview
    ) {
        this.receiverUser = receiverUser;
        this.actorUser = actorUser;
        this.type = type;
        this.postId = postId;
        this.commentId = commentId;
        this.commentPreview = commentPreview;
        this.isRead = false;
    }

    public void markAsRead() {
        if (this.isRead) {
            return;
        }
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
