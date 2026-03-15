package com.wilo.server.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NaverUserInfoResponseDto(
        @JsonProperty("resultcode")
        String resultCode,
        String message,
        Response response

) {
    public record Response(
            String id,
            String email,
            String nickname,
            @JsonProperty("profile_image")
            String profileImage
    ) {}

    public String id() {
        return response != null ? response.id() : null;
    }

    public String email() {
        return response != null ? response.email() : null;
    }

    public String nickname() {
        return response != null ? response.nickname() : null;
    }

    public String profileImage() {
        return response != null ? response.profileImage() : null;
    }
}
