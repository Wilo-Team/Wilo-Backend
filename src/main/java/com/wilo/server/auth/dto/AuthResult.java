package com.wilo.server.auth.dto;

public record AuthResult(
        UserSummaryResponse user,
        TokenPair tokens
) {
}
