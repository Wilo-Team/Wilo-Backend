package com.wilo.server.user.controller;

import com.wilo.server.global.response.CommonResponse;
import com.wilo.server.user.dto.UserResponseDto;
import com.wilo.server.user.dto.UserUpdateRequestDto;
import com.wilo.server.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "User", description = "유저 프로필 API")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 유저의 프로필을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":1005,\"message\":\"로그인이 필요합니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "유저 없음",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":4001,\"message\":\"유저를 찾을 수 없습니다.\"}")
                    )
            )
    })
    public CommonResponse<UserResponseDto> getUserProfile() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return CommonResponse.success(userService.getUserProfile(userId));
    }

    @PatchMapping
    @Operation(summary = "내 프로필 수정", description = "현재 로그인한 유저의 닉네임/한 줄 소개를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값 검증 실패",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":4001,\"message\":\"요청 값이 유효하지 않습니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":1005,\"message\":\"로그인이 필요합니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "닉네임 중복",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":4002,\"message\":\"이미 사용 중인 닉네임입니다.\"}")
                    )
            )
    })
    public CommonResponse<UserResponseDto> updateUserProfile(
            @Valid @RequestBody UserUpdateRequestDto request
    ) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return CommonResponse.success(userService.updateUserProfile(userId, request));
    }

    @PatchMapping("/profile-image")
    @Operation(summary = "프로필 이미지 등록/수정", description = "이미지를 업로드하고 현재 로그인한 유저의 프로필 이미지 URL을 갱신합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "파일 업로드 실패",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":2001,\"message\":\"파일 업로드 중 문제가 발생했습니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":1005,\"message\":\"로그인이 필요합니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "유저 없음",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":4001,\"message\":\"유저를 찾을 수 없습니다.\"}")
                    )
            )
    })
    public CommonResponse<String> updateProfileImage(
            @RequestParam("image") MultipartFile image
    ) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return CommonResponse.success(userService.updateProfileImage(userId, image));
    }
}
