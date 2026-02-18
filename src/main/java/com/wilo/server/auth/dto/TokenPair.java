package com.wilo.server.auth.dto;

public record TokenPair(
        String accessToken,
        String refreshToken
) {
}
