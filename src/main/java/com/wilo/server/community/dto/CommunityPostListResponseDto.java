package com.wilo.server.community.dto;

import java.util.List;

public record CommunityPostListResponseDto(
        List<CommunityPostSummaryDto> items,
        int page,
        int size,
        boolean hasNext
) {
}
