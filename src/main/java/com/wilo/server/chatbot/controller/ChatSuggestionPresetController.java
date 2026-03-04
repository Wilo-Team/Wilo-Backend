package com.wilo.server.chatbot.controller;

import com.wilo.server.chatbot.dto.SuggestionPresetListResponse;
import com.wilo.server.chatbot.service.ChatSuggestionPresetService;
import com.wilo.server.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat/suggestion-presets")
public class ChatSuggestionPresetController {

    private final ChatSuggestionPresetService chatSuggestionPresetService;

    @GetMapping
    @Operation(
            summary = "추천 버튼 프리셋 조회",
            description = """
                선택된 챗봇 유형에 맞는 추천 질문 프리셋 목록을 조회합니다.
                - 채팅 시작 전/입력창 위에 보여주는 고정 프리셋 용도
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 오류",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "404", description = "챗봇 유형 없음",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<SuggestionPresetListResponse> getPresets(
            @Parameter(description = "챗봇 유형 ID", example = "1")
            @Min(value = 1, message = "chatbotTypeId는 1 이상이어야 합니다.")
            @RequestParam Long chatbotTypeId
    ) {
        return CommonResponse.success(chatSuggestionPresetService.getPresets(chatbotTypeId));
    }
}
