package com.wilo.server.notification.controller;

import com.wilo.server.auth.error.AuthErrorCase;
import com.wilo.server.global.exception.ApplicationException;
import com.wilo.server.global.response.CommonResponse;
import com.wilo.server.notification.dto.NotificationListResponseDto;
import com.wilo.server.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Notification", description = "유저 알림 API")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "알림 목록 조회", description = "내 알림을 최신순(createdAt desc, id desc)으로 조회하며 커서 기반 무한 스크롤을 지원합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":1005,\"message\":\"로그인이 필요합니다.\"}")
                    )
            )
    })
    public CommonResponse<NotificationListResponseDto> getNotifications(
            @Parameter(description = "커서 값. 포맷: createdAt|id")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기. 기본값 20, 최대 50")
            @RequestParam(defaultValue = "20") Integer size
    ) {
        Long userId = extractUserId();
        return CommonResponse.success(notificationService.getNotifications(userId, cursor, size));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 단건 읽음 처리", description = "내 알림 1건을 읽음 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":1005,\"message\":\"로그인이 필요합니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "타인 알림 접근",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":7002,\"message\":\"본인의 알림만 처리할 수 있습니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "알림 없음",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":7001,\"message\":\"알림을 찾을 수 없습니다.\"}")
                    )
            )
    })
    public CommonResponse<String> markAsRead(@PathVariable Long notificationId) {
        Long userId = extractUserId();
        notificationService.markAsRead(userId, notificationId);
        return CommonResponse.success("알림이 읽음 처리되었습니다.");
    }

    @PatchMapping("/read-all")
    @Operation(summary = "알림 전체 읽음 처리", description = "내 미읽음 알림 전체를 읽음 처리하고 처리 건수를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "전체 읽음 처리 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":1005,\"message\":\"로그인이 필요합니다.\"}")
                    )
            )
    })
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
