package com.wilo.server.user.controller;

import com.wilo.server.global.response.CommonResponse;
import com.wilo.server.user.dto.GuestChatbotTypeSetRequestDto;
import com.wilo.server.user.dto.GuestChatbotTypeSetResponseDto;
import com.wilo.server.user.dto.GuestNicknameRequestDto;
import com.wilo.server.user.dto.GuestNicknameResponseDto;
import com.wilo.server.user.service.GuestProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/guest/profile")
public class GuestProfileController {

    private final GuestProfileService guestProfileService;

    @PostMapping("/nickname")
    @Operation(
            summary = "게스트 닉네임 설정",
            description = """
                    비로그인 사용자의 닉네임을 설정합니다.
                    - X-Guest-Id 헤더 필수
                    - 최초 호출 시 프로필 생성
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "닉네임 설정 성공"),
            @ApiResponse(responseCode = "400", description = "닉네임 정책 위반",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))
            )
    })
    public CommonResponse<GuestNicknameResponseDto> setNickname(
            @Parameter(description = "비로그인 사용자 식별자(UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader(value = "X-Guest-Id", required = false)
            String guestId,
            @Valid @RequestBody GuestNicknameRequestDto request
    ) {
        return CommonResponse.success(guestProfileService.setNickname(guestId, request));
    }

    @PostMapping("/chatbot-type")
    @Operation(
            summary = "비로그인(게스트) 챗봇 유형 설정",
            description = """
                    비로그인 사용자의 챗봇 유형을 설정합니다.
                    - 게스트 식별자는 X-Guest-Id 헤더로 전달합니다.
                    - chatbotTypeId는 /api/v1/chatbot-types 조회 결과의 id를 사용합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "설정 성공"),
            @ApiResponse(responseCode = "400", description = "게스트 헤더 누락/요청값 검증 실패",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 챗봇 유형",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<GuestChatbotTypeSetResponseDto> setGuestChatbotType(
            @Parameter(description = "비로그인 사용자 식별자(UUID). 비로그인 요청 시 필수", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @Valid @RequestBody GuestChatbotTypeSetRequestDto request
    ) {
        return CommonResponse.success(guestProfileService.setChatbotType(guestId, request));
    }
}
