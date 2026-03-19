package com.wilo.server.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserInfoResponseDto(
        Long id,

        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount,

        Properties properties
) {
    public record KakaoAccount(
            String email
    ) {}

    public record Properties(
            String nickname,
            @JsonProperty("profile_image")
            String profileImage
    ) {}

    public String email() {
        return kakaoAccount != null ? kakaoAccount.email() : null;
    }

    public String nickname() {
        return properties != null ? properties.nickname() : null;
    }

    public String profileImage() {
        return properties != null ? properties.profileImage() : null;
    }
}