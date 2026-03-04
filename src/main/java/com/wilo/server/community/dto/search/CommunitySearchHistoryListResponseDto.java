package com.wilo.server.community.dto.search;

import java.util.List;

public record CommunitySearchHistoryListResponseDto(
        List<CommunitySearchHistoryItemDto> items,
        String cursor,
        int size,
        boolean hasNext,
        String nextCursor
) {
}
