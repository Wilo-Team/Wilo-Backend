package com.wilo.server.community.dto.post;

import com.wilo.server.community.dto.comment.CommunityCommentDto;
import com.wilo.server.community.dto.comment.CommunityPostAuthorDto;
import com.wilo.server.community.entity.post.CommunityCategory;
import java.time.LocalDateTime;
import java.util.List;

public record CommunityPostDetailResponseDto(
        Long id,
        CommunityCategory category,
        String categoryName,
        String title,
        String content,
        LocalDateTime createdAt,
        long daysAgo,
        long viewCount,
        long likeCount,
        long commentCount,
        CommunityPostAuthorDto author,
        List<String> imageUrls,
        List<CommunityCommentDto> comments
) {
}
