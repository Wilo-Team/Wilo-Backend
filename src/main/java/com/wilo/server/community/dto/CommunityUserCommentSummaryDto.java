package com.wilo.server.community.dto;

import java.time.LocalDateTime;

public record CommunityUserCommentSummaryDto(
        Long id,
        Long postId,
        String postTitle,
        String content,
        LocalDateTime createdAt,
        long daysAgo
) {
}
