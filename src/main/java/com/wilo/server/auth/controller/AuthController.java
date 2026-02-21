package com.wilo.server.auth.controller;

import com.wilo.server.auth.dto.LoginRequestDto;
import com.wilo.server.auth.dto.SignUpRequestDto;
import com.wilo.server.auth.dto.TokenRequestDto;
import com.wilo.server.auth.dto.TokenResponseDto;
import com.wilo.server.auth.service.AuthService;
import com.wilo.server.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "회원 인증 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일/비밀번호/닉네임으로 회원가입하고 JWT 토큰을 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "409", description = "이메일 또는 닉네임 중복", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<String> signUp(@Valid @RequestBody SignUpRequestDto request) {
        authService.signUpAndIssueToken(request);
        return CommonResponse.success("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 JWT 토큰을 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return CommonResponse.success(authService.loginAndIssueToken(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "유효한 리프레시 토큰으로 access/refresh 토큰을 재발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<TokenResponseDto> refreshToken(@RequestBody TokenRequestDto tokenRequestDto) {
        return CommonResponse.success(authService.refreshToken(tokenRequestDto));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "access/refresh 토큰 쿠키를 만료 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    public CommonResponse<String> logout(@RequestBody(required = false) TokenRequestDto tokenRequestDto) {
        authService.logout(tokenRequestDto);
        return CommonResponse.success("로그아웃 되었습니다.");
    }
}
