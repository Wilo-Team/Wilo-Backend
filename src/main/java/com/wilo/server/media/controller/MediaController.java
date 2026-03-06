package com.wilo.server.media.controller;

import com.wilo.server.global.response.CommonResponse;
import com.wilo.server.media.dto.MediaUploadResponse;
import com.wilo.server.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "미디어 업로드",
            description = """
                채팅에서 사진 첨부 버튼을 통해 이미지/오디오 파일을 업로드합니다.

                sourceType
                - CHAT
                - COMMUNITY
                - PROFILE
                """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "미디어 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (파일 또는 sourceType 오류)",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "415", description = "지원하지 않는 미디어 타입",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonResponse.class)))
    })
    public CommonResponse<MediaUploadResponse> uploadMedia(

            @Parameter(description = "업로드할 파일", required = true)
            @RequestPart("file") MultipartFile file,

            @Parameter(description = "미디어 사용 목적", example = "CHAT")
            @RequestParam @NotBlank(message = "sourceType은 필수입니다.")
            String sourceType
    ) {
        return CommonResponse.success(mediaService.upload(file, sourceType));
    }
}
