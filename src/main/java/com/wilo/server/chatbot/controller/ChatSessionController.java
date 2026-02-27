package com.wilo.server.chatbot.controller;

import com.wilo.server.chatbot.dto.*;
import com.wilo.server.chatbot.service.ChatSessionService;
import com.wilo.server.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;


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

    @GetMapping
    @Operation(
            summary = "대화 보관함(세션 목록) 조회",
            description = """
                    대화 세션 목록을 커서 기반으로 조회합니다.
                    
                    정렬 기준:
                    - lastMessageAt DESC
                    - id DESC
                    
                    페이징 방식:
                    - 복합 커서(lastMessageAt + id) 기반 Keyset Pagination
                    
                    기본값:
                    - status = ACTIVE
                    - size = 20
                    
                    제한:
                    - size 최대 50
                    
                    인증 정책:
                    - 로그인 사용자 → userId 기반 조회
                    - 비로그인 사용자 → X-Guest-Id 헤더 필요
                    """
    )
    @ApiResponses(value = {

            @ApiResponse(responseCode = "200", description = "조회 성공"),

            @ApiResponse(responseCode = "400", description = "요청 파라미터 오류(size 범위/status 오류 등)",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),

            @ApiResponse(responseCode = "400", description = "비로그인 사용자의 X-Guest-Id 헤더 누락",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),

            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<ChatSessionListResponse> getSessions(
            @Parameter(description = "비로그인 사용자 식별자(UUID). 비로그인 요청 시 필수", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader(value = "X-Guest-Id", required = false)
            String guestId,

            @Parameter(description = "기본 ACTIVE, 옵션: ACTIVE 또는 ARCHIVED", example = "ACTIVE")
            @RequestParam(required = false)
            @Pattern(regexp = "ACTIVE|ARCHIVED", message = "유효하지 않은 상태값입니다.")
            String status,

            @Parameter(description = "이전 페이지 마지막 세션의 lastMessageAt 값", example = "2026-02-19T10:40:00")
            @RequestParam(required = false)
            LocalDateTime cursorLastMessageAt,

            @Parameter(description = "이전 페이지 마지막 세션의 sessionId 값", example = "101")
            @RequestParam(required = false)
            Long cursorId,

            @Parameter(description = "페이지 크기 (기본 20, 최대 50)", example = "20")
            @RequestParam(required = false)
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            Integer size
    ) {
        ChatSessionListRequest request =
                new ChatSessionListRequest(
                        status,
                        cursorLastMessageAt,
                        cursorId,
                        size
                );
        return CommonResponse.success(chatSessionService.getSessions(guestId, request));
    }

    @PatchMapping("/{sessionId}")
    @Operation(
            summary = "세션 제목 수정",
            description = """
                대화 세션 제목을 수정합니다.
                - 로그인 사용자는 user_id 기준
                - 비로그인은 X-Guest-Id 기준
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "제목 정책 위반",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "404", description = "세션 없음",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<ChatSessionTitleUpdateResponse> updateSessionTitle(

            @Parameter(description = "세션 ID", example = "101")
            @Min(value = 1, message = "sessionId는 1 이상이어야 합니다.")
            @PathVariable Long sessionId,
            @Parameter(description = "비로그인 사용자 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @Valid @RequestBody ChatSessionTitleUpdateRequest request
    ) {
        return CommonResponse.success(chatSessionService.updateSessionTitle(sessionId, guestId, request));
    }

    @PatchMapping("/{sessionId}/archive")
    @Operation(
            summary = "세션 보관",
            description = """
                대화 세션을 ARCHIVED 상태로 변경합니다.
                - 로그인 사용자는 user_id 기준
                - 비로그인 사용자는 X-Guest-Id 기준
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "보관 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "404", description = "세션 없음",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<ChatSessionArchiveResponse> archiveSession(
            @Parameter(description = "세션 ID", example = "101")
            @Min(value = 1, message = "sessionId는 1 이상이어야 합니다.")
            @PathVariable Long sessionId,
            @Parameter(description = "비로그인 사용자 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId
    ) {
        return CommonResponse.success(chatSessionService.archiveSession(sessionId, guestId));
    }

    @PatchMapping("/{sessionId}/restore")
    @Operation(
            summary = "세션 복원",
            description = """
                ARCHIVED 상태의 대화 세션을 ACTIVE로 복원합니다.
                - 로그인 사용자는 user_id 기준
                - 비로그인 사용자는 X-Guest-Id 기준
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "복원 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 상태(ARCHIVED가 아님) 또는 요청값 오류",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "404", description = "세션 없음",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<ChatSessionRestoreResponse> restoreSession(
            @Parameter(description = "세션 ID", example = "101")
            @Min(value = 1, message = "sessionId는 1 이상이어야 합니다.")
            @PathVariable Long sessionId,
            @Parameter(description = "비로그인 사용자 UUID. 비로그인 요청 시 필수", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId
    ) {
        return CommonResponse.success(chatSessionService.restoreSession(sessionId, guestId));
    }
}