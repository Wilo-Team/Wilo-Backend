package com.wilo.server.auth.controller;

import com.wilo.server.auth.dto.AuthResponse;
import com.wilo.server.auth.dto.AuthResult;
import com.wilo.server.auth.dto.LoginRequest;
import com.wilo.server.auth.dto.SignUpRequest;
import com.wilo.server.auth.service.AuthService;
import com.wilo.server.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "회원 인증 API")
public class AuthController {

    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일/비밀번호/닉네임으로 회원가입하고 JWT 토큰을 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "409", description = "이메일 또는 닉네임 중복", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public ResponseEntity<CommonResponse<AuthResponse>> signUp(@Valid @RequestBody SignUpRequest request) {

        AuthResult result = authService.signUp(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createTokenCookie(ACCESS_TOKEN_COOKIE_NAME, result.tokens().accessToken(), Duration.ofHours(1)).toString())
                .header(HttpHeaders.SET_COOKIE, createTokenCookie(REFRESH_TOKEN_COOKIE_NAME, result.tokens().refreshToken(), Duration.ofDays(14)).toString())
                .body(CommonResponse.success(new AuthResponse(
                        result.tokens().accessToken(),
                        result.tokens().refreshToken(),
                        result.user()
                )));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 JWT 토큰을 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public ResponseEntity<CommonResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {

        AuthResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createTokenCookie(ACCESS_TOKEN_COOKIE_NAME, result.tokens().accessToken(), Duration.ofHours(1)).toString())
                .header(HttpHeaders.SET_COOKIE, createTokenCookie(REFRESH_TOKEN_COOKIE_NAME, result.tokens().refreshToken(), Duration.ofDays(14)).toString())
                .body(CommonResponse.success(new AuthResponse(
                        result.tokens().accessToken(),
                        result.tokens().refreshToken(),
                        result.user()
                )));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "access/refresh 토큰 쿠키를 만료 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    public ResponseEntity<CommonResponse<?>> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie(ACCESS_TOKEN_COOKIE_NAME).toString())
                .header(HttpHeaders.SET_COOKIE, clearCookie(REFRESH_TOKEN_COOKIE_NAME).toString())
                .body(CommonResponse.success());
    }

    private ResponseCookie createTokenCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie clearCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}
