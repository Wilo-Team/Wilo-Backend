package com.wilo.server.chatbot.controller;

import com.wilo.server.chatbot.dto.ChatMessageFeedbackRequest;
import com.wilo.server.chatbot.dto.ChatMessageFeedbackResponse;
import com.wilo.server.chatbot.service.ChatMessageFeedbackService;
import com.wilo.server.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat/messages")
public class ChatMessageFeedbackController {

    private final ChatMessageFeedbackService chatMessageFeedbackService;

    @PatchMapping("/{messageId}/feedback")
    @Operation(
            summary = "메시지 피드백",
            description = """
                챗봇(BOT) 메시지에 대해 사용자 피드백을 남깁니다.
                - LIKE : 도움이 되었어요 👍
                - DISLIKE : 도움이 안 되었어요 👎
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "피드백 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 피드백 요청 또는 USER 메시지",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "404", description = "메시지 없음",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<ChatMessageFeedbackResponse> giveFeedback(
            @Parameter(description = "메시지 ID", example = "55")
            @Min(value = 1, message = "messageId는 1 이상이어야 합니다.")
            @PathVariable Long messageId,
            @Valid @RequestBody ChatMessageFeedbackRequest request
    ) {
        return CommonResponse.success(chatMessageFeedbackService.giveFeedback(messageId, request));
    }
}
