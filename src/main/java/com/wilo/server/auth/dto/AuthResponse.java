package com.wilo.server.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserSummaryResponse user
) {
}
