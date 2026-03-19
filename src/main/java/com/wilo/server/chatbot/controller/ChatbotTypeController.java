package com.wilo.server.chatbot.controller;

import com.wilo.server.chatbot.dto.ChatbotTypeListResponse;
import com.wilo.server.chatbot.service.ChatbotTypeService;
import com.wilo.server.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chatbot-types")
public class ChatbotTypeController {

    private final ChatbotTypeService chatbotTypeService;

    @GetMapping
    @Operation(
            summary = "챗봇 유형 목록 조회",
            description = "온보딩 및 설정 화면에서 사용되는 챗봇 유형 목록을 조회합니다. (비로그인 허용)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<ChatbotTypeListResponse> getChatbotTypes() {
        return CommonResponse.success(chatbotTypeService.getChatbotTypes());
    }
}
