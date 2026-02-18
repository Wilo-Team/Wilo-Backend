package com.wilo.server.auth.dto;

import com.wilo.server.user.entity.User;

public record UserSummaryResponse(
        Long id,
        String email,
        String nickname,
        String description,
        String profileImageUrl
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getDescription(),
                user.getProfileImageUrl()
        );
    }
}
