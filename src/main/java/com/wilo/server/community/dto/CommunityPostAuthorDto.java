package com.wilo.server.community.dto;

import com.wilo.server.user.entity.User;

public record CommunityPostAuthorDto(
        Long id,
        String nickname,
        String profileImageUrl
) {
    public static CommunityPostAuthorDto from(User user) {
        return new CommunityPostAuthorDto(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }
}
