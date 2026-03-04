package com.wilo.server.notification.service;

import com.wilo.server.community.entity.comment.CommunityComment;
import com.wilo.server.community.entity.post.CommunityPost;
import com.wilo.server.notification.dto.NotificationListResponseDto;
import com.wilo.server.notification.dto.NotificationSummaryDto;
import com.wilo.server.notification.entity.NotificationType;
import com.wilo.server.notification.entity.UserNotification;
import com.wilo.server.notification.error.NotificationErrorCase;
import com.wilo.server.notification.repository.UserNotificationRepository;
import com.wilo.server.global.exception.ApplicationException;
import com.wilo.server.user.entity.User;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int COMMENT_PREVIEW_LENGTH = 120;

    private final UserNotificationRepository userNotificationRepository;

    @Transactional
    public void notifyComment(CommunityPost post, User actorUser, CommunityComment comment) {
        if (post.getUser().getId().equals(actorUser.getId())) {
            return;
        }

        userNotificationRepository.save(
                UserNotification.builder()
                        .receiverUser(post.getUser())
                        .actorUser(actorUser)
                        .type(NotificationType.COMMENT)
                        .postId(post.getId())
                        .commentId(comment.getId())
                        .commentPreview(createPreview(comment.getContent()))
                        .build()
        );
    }

    @Transactional
    public void notifyPostLike(CommunityPost post, User actorUser) {
        if (post.getUser().getId().equals(actorUser.getId())) {
            return;
        }

        userNotificationRepository.save(
                UserNotification.builder()
                        .receiverUser(post.getUser())
                        .actorUser(actorUser)
                        .type(NotificationType.POST_LIKE)
                        .postId(post.getId())
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public NotificationListResponseDto getNotifications(Long receiverUserId, String cursor, Integer size) {
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(0, safeSize + 1);

        Cursor parsedCursor = Cursor.from(cursor);
        List<UserNotification> fetchedNotifications = userNotificationRepository.findLatestByReceiverWithCursor(
                receiverUserId,
                parsedCursor.createdAt(),
                parsedCursor.id(),
                pageable
        );

        boolean hasNext = fetchedNotifications.size() > safeSize;
        List<UserNotification> pageNotifications = hasNext
                ? fetchedNotifications.subList(0, safeSize)
                : fetchedNotifications;

        List<NotificationSummaryDto> items = pageNotifications.stream()
                .map(this::toDto)
                .toList();

        String nextCursor = null;
        if (hasNext && !pageNotifications.isEmpty()) {
            UserNotification last = pageNotifications.get(pageNotifications.size() - 1);
            nextCursor = Cursor.of(last).toCursorValue();
        }

        return new NotificationListResponseDto(items, cursor, safeSize, hasNext, nextCursor);
    }

    @Transactional
    public void markAsRead(Long receiverUserId, Long notificationId) {
        UserNotification notification = userNotificationRepository.findById(notificationId)
                .orElseThrow(() -> ApplicationException.from(NotificationErrorCase.NOTIFICATION_NOT_FOUND));

        if (!notification.getReceiverUser().getId().equals(receiverUserId)) {
            throw ApplicationException.from(NotificationErrorCase.FORBIDDEN_NOTIFICATION_ACCESS);
        }

        notification.markAsRead();
    }

    @Transactional
    public int markAllAsRead(Long receiverUserId) {
        return userNotificationRepository.markAllAsRead(receiverUserId, LocalDateTime.now());
    }

    @Transactional
    public void deleteNotification(Long receiverUserId, Long notificationId) {
        UserNotification notification = userNotificationRepository.findById(notificationId)
                .orElseThrow(() -> ApplicationException.from(NotificationErrorCase.NOTIFICATION_NOT_FOUND));

        if (!notification.getReceiverUser().getId().equals(receiverUserId)) {
            throw ApplicationException.from(NotificationErrorCase.FORBIDDEN_NOTIFICATION_ACCESS);
        }

        userNotificationRepository.delete(notification);
    }

    @Transactional
    public int deleteAllUserNotifications(Long receiverUserId) {
        return userNotificationRepository.deleteAllByReceiverUserId(receiverUserId);
    }

    private NotificationSummaryDto toDto(UserNotification notification) {
        String actorNickname = notification.getActorUser().getNickname();
        String message = switch (notification.getType()) {
            case COMMENT -> actorNickname + "님이 회원님의 글에 댓글을 남겼습니다.";
            case POST_LIKE -> actorNickname + "님이 회원님의 글을 좋아합니다.";
        };

        return new NotificationSummaryDto(
                notification.getId(),
                notification.getType(),
                message,
                notification.getPostId(),
                notification.getCommentId(),
                notification.getCommentPreview(),
                actorNickname,
                notification.isRead(),
                calculateTimeAgo(notification.getCreatedAt()),
                notification.getCreatedAt()
        );
    }

    private String createPreview(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= COMMENT_PREVIEW_LENGTH) {
            return content;
        }
        return content.substring(0, COMMENT_PREVIEW_LENGTH) + "...";
    }

    private String calculateTimeAgo(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "0분 전";
        }

        long minutes = Math.max(0, Duration.between(createdAt, LocalDateTime.now()).toMinutes());
        if (minutes < 60) {
            return Math.max(1, minutes) + "분 전";
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "시간 전";
        }

        long days = hours / 24;
        return days + "일 전";
    }

    private record Cursor(LocalDateTime createdAt, Long id) {
        private static Cursor from(String cursor) {
            if (cursor == null || cursor.isBlank()) {
                return new Cursor(null, null);
            }

            String[] parts = cursor.split("\\|");
            if (parts.length != 2) {
                return new Cursor(null, null);
            }

            try {
                return new Cursor(LocalDateTime.parse(parts[0]), Long.parseLong(parts[1]));
            } catch (RuntimeException e) {
                return new Cursor(null, null);
            }
        }

        private static Cursor of(UserNotification notification) {
            return new Cursor(notification.getCreatedAt(), notification.getId());
        }

        private String toCursorValue() {
            return createdAt + "|" + id;
        }
    }
}
