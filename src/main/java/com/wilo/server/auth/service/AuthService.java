package com.wilo.server.auth.service;

import com.wilo.server.auth.dto.AuthResult;
import com.wilo.server.auth.dto.LoginRequest;
import com.wilo.server.auth.dto.SignUpRequest;
import com.wilo.server.auth.dto.TokenPair;
import com.wilo.server.auth.dto.UserSummaryResponse;
import com.wilo.server.auth.error.AuthErrorCase;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResult signUp(SignUpRequest request) {

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

        TokenPair tokenPair = issueTokens(user.getId());
        return new AuthResult(UserSummaryResponse.from(user), tokenPair);
    }

    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> ApplicationException.from(AuthErrorCase.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw ApplicationException.from(AuthErrorCase.INVALID_CREDENTIALS);
        }

        TokenPair tokenPair = issueTokens(user.getId());
        return new AuthResult(UserSummaryResponse.from(user), tokenPair);
    }

    private TokenPair issueTokens(Long userId) {
        return new TokenPair(
                jwtTokenProvider.generateAccessToken(userId),
                jwtTokenProvider.generateRefreshToken(userId)
        );
    }
}
