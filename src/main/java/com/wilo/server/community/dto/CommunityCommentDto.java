package com.wilo.server.community.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record CommunityCommentDto(
        Long id,
        CommunityPostAuthorDto author,
        String content,
        long daysAgo,
        LocalDateTime createdAt,
        List<CommunityCommentDto> replies
) {
    public static CommunityCommentDto of(
            Long id,
            CommunityPostAuthorDto author,
            String content,
            long daysAgo,
            LocalDateTime createdAt
    ) {
        return new CommunityCommentDto(id, author, content, daysAgo, createdAt, new ArrayList<>());
    }
}
