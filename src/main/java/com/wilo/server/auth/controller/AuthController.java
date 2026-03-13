package com.wilo.server.auth.controller;

import com.wilo.server.auth.dto.AppleLoginRequestDto;
import com.wilo.server.auth.dto.AppleWithdrawRequestDto;
import com.wilo.server.auth.dto.LoginRequestDto;
import com.wilo.server.auth.dto.PhoneVerificationConfirmRequestDto;
import com.wilo.server.auth.dto.PhoneVerificationSendRequestDto;
import com.wilo.server.auth.dto.SignUpRequestDto;
import com.wilo.server.auth.dto.TokenRequestDto;
import com.wilo.server.auth.dto.TokenResponseDto;
import com.wilo.server.auth.error.AuthErrorCase;
import com.wilo.server.auth.service.AuthService;
import com.wilo.server.global.exception.ApplicationException;
import com.wilo.server.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "회원 인증 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "전화번호/비밀번호로 회원가입하고 JWT 토큰을 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "409", description = "전화번호 중복", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<String> signUp(@Valid @RequestBody SignUpRequestDto request) {
        authService.signUpAndIssueToken(request);
        return CommonResponse.success("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "전화번호/비밀번호로 로그인하고 JWT 토큰을 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return CommonResponse.success(authService.loginAndIssueToken(request));
    }

    @PostMapping("/apple/login")
    @Operation(summary = "Apple 로그인", description = "Apple accessToken 검증 후 로그인/회원가입 및 JWT 토큰을 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "401", description = "Apple accessToken 검증 실패", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<TokenResponseDto> appleLogin(@Valid @RequestBody AppleLoginRequestDto request) {
        return CommonResponse.success(authService.loginWithAppleAndIssueToken(request));
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

    @DeleteMapping("/apple/withdraw")
    @Operation(summary = "Apple 회원 탈퇴", description = "authorizationCode를 사용해 Apple 토큰을 철회하고 회원을 탈퇴 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<String> withdrawAppleAccount(@Valid @RequestBody AppleWithdrawRequestDto request) {
        Long userId = extractAuthenticatedUserId();
        authService.withdrawAppleAccount(userId, request);
        return CommonResponse.success("회원 탈퇴가 완료되었습니다.");
    }

    @PostMapping("/phone-verification/send")
    @Operation(
            summary = "휴대폰 인증번호 발송",
            description = """
                    입력한 전화번호로 6자리 인증번호를 발송합니다.
                    인증번호는 Redis에 3분 TTL로 저장됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증번호 발송 성공"),
            @ApiResponse(responseCode = "400", description = "전화번호 형식 오류", content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "500", description = "문자 발송 실패 또는 NCP SENS 설정 누락", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<String> sendPhoneVerificationCode(
            @Valid @RequestBody PhoneVerificationSendRequestDto request
    ) {
        authService.sendPhoneVerificationCode(request);
        return CommonResponse.success("인증번호가 발송되었습니다.");
    }

    @PostMapping("/phone-verification/resend")
    @Operation(
            summary = "휴대폰 인증번호 재발송",
            description = """
                    전화번호로 인증번호를 재발송합니다.
                    기존 인증번호가 유효기간 내에 남아있어도 즉시 무효화하고, 새로운 6자리 인증번호를 발급해 발송합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증번호 재발송 성공"),
            @ApiResponse(responseCode = "400", description = "전화번호 형식 오류", content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "500", description = "문자 발송 실패 또는 NCP SENS 설정 누락", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<String> resendPhoneVerificationCode(
            @Valid @RequestBody PhoneVerificationSendRequestDto request
    ) {
        authService.resendPhoneVerificationCode(request);
        return CommonResponse.success("인증번호가 재발송되었습니다.");
    }

    @PostMapping("/phone-verification/confirm")
    @Operation(
            summary = "휴대폰 인증번호 검증",
            description = """
                    전화번호와 인증번호를 검증합니다.
                    Redis에 저장된 인증번호(3분 TTL)와 일치하면 인증 성공 처리되고 해당 코드는 즉시 삭제됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증번호 검증 성공"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류 / 인증번호 만료 / 인증번호 불일치", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<String> confirmPhoneVerificationCode(
            @Valid @RequestBody PhoneVerificationConfirmRequestDto request
    ) {
        authService.confirmPhoneVerificationCode(request);
        return CommonResponse.success("전화번호 인증이 완료되었습니다.");
    }

    private Long extractAuthenticatedUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;

        if (principal instanceof Long userId) {
            return userId;
        }

        throw ApplicationException.from(AuthErrorCase.UNAUTHORIZED);
    }
}
