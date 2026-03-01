package com.wilo.server.notification.controller;

import com.wilo.server.auth.error.AuthErrorCase;
import com.wilo.server.global.exception.ApplicationException;
import com.wilo.server.global.response.CommonResponse;
import com.wilo.server.notification.dto.NotificationListResponseDto;
import com.wilo.server.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public CommonResponse<NotificationListResponseDto> getNotifications(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        Long userId = extractUserId();
        return CommonResponse.success(notificationService.getNotifications(userId, cursor, size));
    }

    @PatchMapping("/{notificationId}/read")
    public CommonResponse<String> markAsRead(@PathVariable Long notificationId) {
        Long userId = extractUserId();
        notificationService.markAsRead(userId, notificationId);
        return CommonResponse.success("알림이 읽음 처리되었습니다.");
    }

    @PatchMapping("/read-all")
    public CommonResponse<Integer> markAllAsRead() {
        Long userId = extractUserId();
        return CommonResponse.success(notificationService.markAllAsRead(userId));
    }

    private Long extractUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;

        if (principal instanceof Long userId) {
            return userId;
        }

        if (principal instanceof String userIdText) {
            try {
                return Long.parseLong(userIdText);
            } catch (NumberFormatException ignored) {
            }
        }

        throw ApplicationException.from(AuthErrorCase.UNAUTHORIZED);
    }
}
