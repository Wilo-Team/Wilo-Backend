package com.wilo.server.notification.dto;

import com.wilo.server.notification.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationSummaryDto(
        Long id,
        NotificationType type,
        String message,
        Long postId,
        Long commentId,
        String commentPreview,
        String actorNickname,
        boolean isRead,
        LocalDateTime createdAt
) {
}
