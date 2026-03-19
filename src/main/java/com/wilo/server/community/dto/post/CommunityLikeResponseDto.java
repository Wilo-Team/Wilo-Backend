package com.wilo.server.community.dto.post;

public record CommunityLikeResponseDto(
        boolean liked,
        long likeCount
) {
}
