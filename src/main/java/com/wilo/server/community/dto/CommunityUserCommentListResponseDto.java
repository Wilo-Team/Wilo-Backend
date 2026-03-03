package com.wilo.server.community.dto;

import java.util.List;

public record CommunityUserCommentListResponseDto(
        List<CommunityUserCommentSummaryDto> items,
        String cursor,
        int size,
        boolean hasNext,
        String nextCursor
) {
}
