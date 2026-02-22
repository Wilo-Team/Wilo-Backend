package com.wilo.server.chatbot.controller;

import com.wilo.server.chatbot.dto.ChatSessionDetailResponse;
import com.wilo.server.chatbot.service.ChatSessionQueryService;
import com.wilo.server.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat/sessions")
public class ChatSessionDetailController {

    private final ChatSessionQueryService chatSessionQueryService;

    @GetMapping("/{sessionId}")
    @Operation(
            summary = "세션 상세 조회(메시지 목록)",
            description = """
                    대화 세션 메타 정보 + 메시지 목록을 조회합니다.
                    - 메시지 페이징: messageId DESC 커서 기반
                    - 비로그인 사용자는 X-Guest-Id 헤더 필수
                    - 세션 소유자만 접근 가능
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "파라미터 오류(size/cursor)",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음(남의 세션 접근)",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "404", description = "세션 없음",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<ChatSessionDetailResponse> getSessionDetail(
            @Parameter(description = "세션 ID", example = "101")
            @PathVariable Long sessionId,

            @Parameter(description = "비로그인 사용자 식별자(UUID). 비로그인 요청 시 필수", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,

            @Parameter(description = "메시지 커서(기준: messageId DESC)", example = "9000")
            @RequestParam(required = false) Long cursor,

            @Parameter(description = "기본 30, 최대 100", example = "30")
            @RequestParam(required = false) Integer size
    ) {
        return CommonResponse.success(
                chatSessionQueryService.getSessionDetail(sessionId, guestId, cursor, size)
        );
    }
}
