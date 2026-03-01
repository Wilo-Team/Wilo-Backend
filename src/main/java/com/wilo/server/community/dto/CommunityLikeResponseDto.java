package com.wilo.server.community.dto;

public record CommunityLikeResponseDto(
        boolean liked,
        long likeCount
) {
}
