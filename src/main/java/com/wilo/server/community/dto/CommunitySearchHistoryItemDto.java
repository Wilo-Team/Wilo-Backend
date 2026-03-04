package com.wilo.server.community.dto;

import java.time.LocalDateTime;

public record CommunitySearchHistoryItemDto(
        Long id,
        String keyword,
        LocalDateTime lastSearchedAt
) {
}
