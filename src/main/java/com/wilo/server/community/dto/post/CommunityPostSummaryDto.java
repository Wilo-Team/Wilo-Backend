package com.wilo.server.community.dto.post;

import com.wilo.server.community.entity.post.CommunityCategory;
import java.time.LocalDateTime;

public record CommunityPostSummaryDto(
        Long id,
        CommunityCategory category,
        String categoryName,
        String title,
        String contentPreview,
        LocalDateTime createdAt,
        long daysAgo,
        long viewCount,
        long likeCount,
        long commentCount
) {
}
