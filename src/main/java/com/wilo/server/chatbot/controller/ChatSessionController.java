package com.wilo.server.chatbot.controller;

import com.wilo.server.chatbot.dto.ChatSessionCreateRequest;
import com.wilo.server.chatbot.dto.ChatSessionCreateResponse;
import com.wilo.server.chatbot.service.ChatSessionService;
import com.wilo.server.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat/sessions")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    @PostMapping
    @Operation(
            summary = "새 대화 시작(세션 생성)",
            description = "채팅 대화 세션을 생성합니다. 로그인 사용자는 user_id로, 비로그인 사용자는 X-Guest-Id로 세션을 생성합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "세션 생성 성공"),
            @ApiResponse(responseCode = "400", description = "게스트 헤더 누락/요청값 검증 실패",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 챗봇 유형",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<ChatSessionCreateResponse> createSession(
            @Parameter(description = "비로그인 사용자 식별자(UUID). 비로그인 요청 시 필수", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @Valid @RequestBody ChatSessionCreateRequest request
    ) {
        return CommonResponse.success(chatSessionService.createSession(request, guestId));
    }
}