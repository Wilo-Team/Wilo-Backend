package com.wilo.server.community.dto;

import java.util.List;

public record CommunityPostListResponseDto(
        List<CommunityPostSummaryDto> items,
        String cursor,
        int size,
        boolean hasNext,
        String nextCursor
) {
}
