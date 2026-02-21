package com.wilo.server.auth.service;

import com.wilo.server.auth.dto.LoginRequestDto;
import com.wilo.server.auth.dto.SignUpRequestDto;
import com.wilo.server.auth.dto.TokenRequestDto;
import com.wilo.server.auth.dto.TokenResponseDto;
import com.wilo.server.auth.error.AuthErrorCase;
import com.wilo.server.auth.repository.RefreshTokenRepository;
import com.wilo.server.user.entity.User;
import com.wilo.server.user.repository.UserRepository;
import com.wilo.server.global.config.security.jwt.JwtTokenProvider;
import com.wilo.server.global.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenResponseDto signUpAndIssueToken(SignUpRequestDto request) {

        if (userRepository.existsByEmail(request.email())) {
            throw ApplicationException.from(AuthErrorCase.EMAIL_ALREADY_EXISTS);
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw ApplicationException.from(AuthErrorCase.NICKNAME_ALREADY_EXISTS);
        }

        User user = userRepository.save(
                User.builder()
                        .email(request.email())
                        .password(passwordEncoder.encode(request.password()))
                        .nickname(request.nickname())
                        .description(request.description())
                        .profileImageUrl(request.profileImageUrl())
                        .build()
        );

        return issueAndSaveTokens(user.getId());
    }

    @Transactional
    public TokenResponseDto loginAndIssueToken(LoginRequestDto request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> ApplicationException.from(AuthErrorCase.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw ApplicationException.from(AuthErrorCase.INVALID_CREDENTIALS);
        }

        return issueAndSaveTokens(user.getId());
    }

    @Transactional
    public TokenResponseDto refreshToken(TokenRequestDto tokenRequestDto) {
        String refreshToken = tokenRequestDto.getRefreshToken();

        if (refreshToken == null || refreshToken.isBlank() || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw ApplicationException.from(AuthErrorCase.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);

        String savedRefreshToken = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> ApplicationException.from(AuthErrorCase.INVALID_REFRESH_TOKEN));

        if (!refreshToken.equals(savedRefreshToken)) {
            throw ApplicationException.from(AuthErrorCase.INVALID_REFRESH_TOKEN);
        }

        return issueAndSaveTokens(userId);
    }

    @Transactional
    public void logout(TokenRequestDto tokenRequestDto) {
        if (tokenRequestDto == null || tokenRequestDto.getRefreshToken() == null) {
            return;
        }

        String refreshToken = tokenRequestDto.getRefreshToken();
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            return;
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        refreshTokenRepository.deleteByUserId(userId);
    }

    private TokenResponseDto issueAndSaveTokens(Long userId) {
        String accessToken = jwtTokenProvider.generateAccessToken(userId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        long refreshTtlMillis = jwtTokenProvider.getTokenExpiry(refreshToken) - System.currentTimeMillis();
        if (refreshTtlMillis <= 0) {
            throw ApplicationException.from(AuthErrorCase.INVALID_REFRESH_TOKEN);
        }

        refreshTokenRepository.save(userId, refreshToken, refreshTtlMillis);

        return TokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
