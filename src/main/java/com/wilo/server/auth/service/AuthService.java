package com.wilo.server.auth.service;

import com.wilo.server.auth.dto.*;
import com.wilo.server.auth.error.AuthErrorCase;
import com.wilo.server.auth.repository.PhoneVerificationCodeRepository;
import com.wilo.server.auth.repository.RefreshTokenRepository;
import com.wilo.server.user.entity.AuthProvider;
import com.wilo.server.user.entity.User;
import com.wilo.server.user.repository.UserRepository;
import com.wilo.server.global.config.security.jwt.JwtTokenProvider;
import com.wilo.server.global.exception.ApplicationException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PhoneVerificationCodeRepository phoneVerificationCodeRepository;
    private final NcpSensSmsService ncpSensSmsService;
    private final AppleOAuthClient appleOAuthClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final NaverOAuthClient naverOAuthClient;

    @Transactional
    public TokenResponseDto signUpAndIssueToken(SignUpRequestDto request) {
        String normalizedPhoneNumber = normalizePhoneNumber(request.phoneNumber());

        if (userRepository.existsByPhoneNumber(normalizedPhoneNumber)) {
            throw ApplicationException.from(AuthErrorCase.PHONE_ALREADY_EXISTS);
        }

        User user = userRepository.save(
                User.builder()
                        .email(buildEmailFromPhone(normalizedPhoneNumber))
                        .password(passwordEncoder.encode(request.password()))
                        .nickname(buildNicknameFromPhone(normalizedPhoneNumber))
                        .description(null)
                        .profileImageUrl(null)
                        .phoneNumber(normalizedPhoneNumber)
                        .phoneVerified(false)
                        .authProvider(AuthProvider.LOCAL)
                        .providerUserId(null)
                        .build()
        );

        return issueAndSaveTokens(user.getId());
    }

    @Transactional
    public TokenResponseDto loginAndIssueToken(LoginRequestDto request) {
        String normalizedPhoneNumber = normalizePhoneNumber(request.phoneNumber());

        User user = userRepository.findByPhoneNumber(normalizedPhoneNumber)
                .orElseThrow(() -> ApplicationException.from(AuthErrorCase.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw ApplicationException.from(AuthErrorCase.INVALID_CREDENTIALS);
        }

        return issueAndSaveTokens(user.getId());
    }

    @Transactional
    public TokenResponseDto loginWithAppleAndIssueToken(AppleLoginRequestDto request) {
        AppleOAuthClient.AppleTokenExchangeResult exchangeResult =
                appleOAuthClient.exchangeAuthorizationCode(request.authorizationCode());
        AppleOAuthClient.AppleIdentityClaims exchangedClaims =
                appleOAuthClient.verifyIdentityToken(exchangeResult.identityToken());
        AppleOAuthClient.AppleIdentityClaims clientClaims =
                appleOAuthClient.verifyIdentityToken(request.identityToken());

        if (!exchangedClaims.subject().equals(clientClaims.subject())) {
            throw ApplicationException.from(AuthErrorCase.APPLE_ACCESS_TOKEN_INVALID);
        }

        String appleSyntheticEmail = buildAppleEmailFromSubject(exchangedClaims.subject());

        User user = userRepository.findByAuthProviderAndProviderUserId(AuthProvider.APPLE, exchangedClaims.subject())
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(appleSyntheticEmail)
                                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                                .nickname(buildUniqueAppleNickname(exchangedClaims.subject()))
                                .description(null)
                                .profileImageUrl(null)
                                .phoneNumber(null)
                                .phoneVerified(false)
                                .authProvider(AuthProvider.APPLE)
                                .providerUserId(exchangedClaims.subject())
                                .build()
                ));

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

    @Transactional
    public void withdrawAppleAccount(Long userId, AppleWithdrawRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApplicationException.from(AuthErrorCase.UNAUTHORIZED));

        appleOAuthClient.revokeByAuthorizationCode(request.authorizationCode());
        refreshTokenRepository.deleteByUserId(userId);
        userRepository.delete(user);
    }

    @Transactional
    public TokenResponseDto loginWithKakaoAndIssueToken(SocialLoginRequestDto request) {
        KakaoUserInfoResponseDto userInfo = kakaoOAuthClient.getUserInfo(request.accessToken());

        String providerUserId = String.valueOf(userInfo.id());
        String email = userInfo.email();
        String nickname = userInfo.nickname();
        String profileImage = userInfo.profileImage();

        User user = userRepository
                .findByAuthProviderAndProviderUserId(AuthProvider.KAKAO, providerUserId)
                .orElseGet(() -> createSocialUser(AuthProvider.KAKAO, providerUserId, email, nickname, profileImage));


        return issueAndSaveTokens(user.getId());
    }

    @Transactional
    public TokenResponseDto loginWithNaverAndIssueToken(SocialLoginRequestDto request) {
        NaverUserInfoResponseDto userInfo = naverOAuthClient.getUserInfo(request.accessToken());

        String providerUserId = userInfo.id();
        String email = userInfo.email();
        String nickname = userInfo.nickname();
        String profileImage = userInfo.profileImage();

        User user = userRepository
                .findByAuthProviderAndProviderUserId(AuthProvider.NAVER, providerUserId)
                .orElseGet(() -> createSocialUser(AuthProvider.NAVER, providerUserId, email, nickname, profileImage));

        return issueAndSaveTokens(user.getId());
    }

    @Transactional
    public void sendPhoneVerificationCode(PhoneVerificationSendRequestDto request) {
        String normalizedPhoneNumber = normalizePhoneNumber(request.phoneNumber());
        issuePhoneVerificationCode(normalizedPhoneNumber);
    }

    @Transactional
    public void resendPhoneVerificationCode(PhoneVerificationSendRequestDto request) {
        String normalizedPhoneNumber = normalizePhoneNumber(request.phoneNumber());
        phoneVerificationCodeRepository.deleteByPhoneNumber(normalizedPhoneNumber);
        issuePhoneVerificationCode(normalizedPhoneNumber);
    }

    private void issuePhoneVerificationCode(String normalizedPhoneNumber) {
        String verificationCode = createVerificationCode();

        ncpSensSmsService.sendVerificationCode(normalizedPhoneNumber, verificationCode);
        phoneVerificationCodeRepository.save(normalizedPhoneNumber, verificationCode);
    }

    @Transactional
    public void confirmPhoneVerificationCode(PhoneVerificationConfirmRequestDto request) {
        String normalizedPhoneNumber = normalizePhoneNumber(request.phoneNumber());

        String savedCode = phoneVerificationCodeRepository.findByPhoneNumber(normalizedPhoneNumber)
                .orElseThrow(() -> ApplicationException.from(AuthErrorCase.PHONE_VERIFICATION_CODE_EXPIRED));

        if (!savedCode.equals(request.verificationCode())) {
            throw ApplicationException.from(AuthErrorCase.PHONE_VERIFICATION_CODE_MISMATCH);
        }

        phoneVerificationCodeRepository.deleteByPhoneNumber(normalizedPhoneNumber);
        userRepository.findAllByPhoneNumber(normalizedPhoneNumber)
                .forEach(User::markPhoneVerified);
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

    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber.replaceAll("\\D", "");
    }

    private String buildEmailFromPhone(String normalizedPhoneNumber) {
        return "phone-" + normalizedPhoneNumber + "@wilo.local";
    }

    private String buildNicknameFromPhone(String normalizedPhoneNumber) {
        return "user_" + normalizedPhoneNumber;
    }

    private String buildAppleEmailFromSubject(String appleSubject) {
        return "apple-" + sha256Hex(appleSubject).substring(0, 24) + "@wilo.local";
    }

    private String buildUniqueAppleNickname(String appleSubject) {
        String seed = sha256Hex(appleSubject).substring(0, 8);
        String base = "apple_" + seed;
        String candidate = base;
        int suffix = 0;

        while (userRepository.existsByNickname(candidate)) {
            suffix++;
            candidate = base + "_" + suffix;
        }

        return candidate;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    private String createVerificationCode() {
        int code = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private User createSocialUser(
            AuthProvider authProvider,
            String providerUserId,
            String email,
            String nickname,
            String profileImage
    ) {
        String finalNickname = resolveUniqueNickname(nickname);

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .nickname(finalNickname)
                .description(null)
                .profileImageUrl(profileImage)
                .phoneNumber(null)
                .phoneVerified(false)
                .authProvider(authProvider)
                .providerUserId(providerUserId)
                .build();

        return userRepository.save(user);
    }

    private String resolveUniqueNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return generateRandomNickname();
        }
        if (userRepository.existsByNickname(nickname)) {
            return generateRandomNickname();
        }
        return nickname;
    }

    private String generateRandomNickname() {
        String candidate;

        do {
            candidate = "user_" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8);
        } while (userRepository.existsByNickname(candidate));

        return candidate;
    }
}
