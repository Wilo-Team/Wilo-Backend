package com.wilo.server.chatbot.controller;

import com.wilo.server.chatbot.dto.ChatGreetingResponse;
import com.wilo.server.chatbot.service.ChatGreetingService;
import com.wilo.server.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat/sessions")
public class ChatGreetingController {

    private final ChatGreetingService chatGreetingService;

    @PostMapping("/{sessionId}/greeting")
    @Operation(
            summary = "챗봇 첫 대화 생성",
            description = """
                채팅방 첫 진입 시 AI 서버 /greeting을 호출하여 챗봇의 첫 인사 메시지를 생성합니다.
                - 로그인 사용자는 JWT userId 기준
                - 비로그인 사용자는 X-Guest-Id 기준
                - 이미 세션에 메시지가 존재하면 greeting을 중복 생성하지 않고 기존 첫 BOT 메시지를 반환합니다.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "greeting 생성/조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 게스트 식별자 누락",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "403", description = "세션 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "404", description = "세션 없음",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "502", description = "AI 서버 오류/응답 형식 불일치",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<ChatGreetingResponse> createGreeting(
            @Parameter(description = "세션 ID", example = "101")
            @Min(value = 1, message = "sessionId는 1 이상이어야 합니다.")
            @PathVariable Long sessionId,
            @Parameter(description = "비로그인 사용자 UUID. 비로그인 요청 시 필수", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId
    ) {
        return CommonResponse.success(
                chatGreetingService.createOrGetGreeting(sessionId, guestId)
        );
    }
}
