package com.wilo.server.notification.dto;

import java.util.List;

public record NotificationListResponseDto(
        List<NotificationSummaryDto> items,
        String cursor,
        int size,
        boolean hasNext,
        String nextCursor
) {
}
