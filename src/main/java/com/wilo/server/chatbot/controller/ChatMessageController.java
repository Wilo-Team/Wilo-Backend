package com.wilo.server.chatbot.controller;

import com.wilo.server.chatbot.dto.ChatMessageSendRequest;
import com.wilo.server.chatbot.dto.ChatMessageSendResponse;
import com.wilo.server.chatbot.service.ChatMessageService;
import com.wilo.server.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat/sessions")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @PostMapping("/{sessionId}/messages")
    @Operation(
            summary = "메시지 전송(사용자 → AI → 응답 저장 후 반환)",
            description = """
                    사용자가 입력한 메시지를 저장하고, 백엔드가 AI 서버 /chat을 호출하여
                    봇 답변을 생성/저장한 뒤 USER/BOT 메시지를 함께 반환합니다. 
                    
                    - 로그인 사용자: JWT userId 기반
                    - 비로그인 사용자: X-Guest-Id 헤더 기반
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "전송 성공"),
            @ApiResponse(responseCode = "400", description = "게스트 헤더 누락/요청값 검증 실패",
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
    public CommonResponse<ChatMessageSendResponse> sendMessage(
            @Parameter(description = "대화 세션 ID", example = "101")
            @Min(value = 1, message = "sessionId는 1 이상이어야 합니다.")
            @PathVariable Long sessionId,
            @Parameter(description = "비로그인 사용자 식별자(UUID). 비로그인 요청 시 필수", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader(value = "X-Guest-Id", required = false)
            String guestId,
            @Valid @RequestBody ChatMessageSendRequest request
    ) {
        return CommonResponse.success(
                chatMessageService.sendMessage(sessionId, guestId, request)
        );
    }
}
