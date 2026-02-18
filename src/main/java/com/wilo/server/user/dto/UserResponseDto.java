package com.wilo.server.user.dto;

import com.wilo.server.user.entity.User;

public record UserResponseDto(
        Long id,
        String email,
        String nickname,
        String description,
        String profileImageUrl
) {
    public static UserResponseDto from(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getDescription(),
                user.getProfileImageUrl()
        );
    }
}
