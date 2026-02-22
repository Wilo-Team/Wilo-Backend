package com.wilo.server.community.controller;

import com.wilo.server.auth.error.AuthErrorCase;
import com.wilo.server.community.dto.CommunityCommentCreateRequestDto;
import com.wilo.server.community.dto.CommunityCommentDto;
import com.wilo.server.community.dto.CommunityLikeResponseDto;
import com.wilo.server.community.dto.CommunityPostCreateRequestDto;
import com.wilo.server.community.dto.CommunityPostDetailResponseDto;
import com.wilo.server.community.dto.CommunityPostListResponseDto;
import com.wilo.server.community.dto.CommunityPostUpdateRequestDto;
import com.wilo.server.community.entity.CommunityCategory;
import com.wilo.server.community.entity.CommunityPostSortType;
import com.wilo.server.community.service.CommunityService;
import com.wilo.server.global.exception.ApplicationException;
import com.wilo.server.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community")
@Tag(name = "Community", description = "커뮤니티 API")
public class CommunityController {

    private final CommunityService communityService;

    @PostMapping("/posts")
    @Operation(summary = "게시글 작성", description = "카테고리/제목/내용/이미지 URL로 게시글을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "작성 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":1005,\"message\":\"로그인이 필요합니다.\"}")
                    )
            )
    })
    public CommonResponse<Long> createPost(@Valid @RequestBody CommunityPostCreateRequestDto request) {
        Long userId = extractUserId();
        return CommonResponse.success(communityService.createPost(userId, request));
    }

    @PatchMapping("/posts/{postId}")
    @Operation(summary = "게시글 수정", description = "본인이 작성한 게시글의 카테고리/제목/내용/이미지를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":1005,\"message\":\"로그인이 필요합니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "작성자만 수정 가능",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":5005,\"message\":\"본인이 작성한 게시글만 수정할 수 있습니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글 없음",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":5001,\"message\":\"게시글을 찾을 수 없습니다.\"}")
                    )
            )
    })
    public CommonResponse<Long> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody CommunityPostUpdateRequestDto request
    ) {
        Long userId = extractUserId();
        return CommonResponse.success(communityService.updatePost(userId, postId, request));
    }

    @DeleteMapping("/posts/{postId}")
    @Operation(summary = "게시글 삭제", description = "본인이 작성한 게시글을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":1005,\"message\":\"로그인이 필요합니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "작성자만 삭제 가능",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":5006,\"message\":\"본인이 작성한 게시글만 삭제할 수 있습니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글 없음",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":5001,\"message\":\"게시글을 찾을 수 없습니다.\"}")
                    )
            )
    })
    public CommonResponse<String> deletePost(@PathVariable Long postId) {
        Long userId = extractUserId();
        communityService.deletePost(userId, postId);
        return CommonResponse.success("게시글이 삭제되었습니다.");
    }

    @GetMapping("/posts")
    @Operation(summary = "게시글 목록 조회", description = "카테고리별(미지정 시 홈 전체), 최신순/추천순, 검색어 기반으로 커서 페이지네이션 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public CommonResponse<CommunityPostListResponseDto> getPosts(
            @Parameter(
                    description = "카테고리 값. 미지정 시 홈 전체 조회. 가능한 값: TREE_SHADE(나무그늘), SUNNY_PLACE(볕드는 곳), HELP_BRANCH(도움가지), SUPPORT_ROOT(버팀뿌리)"
            )
            @RequestParam(required = false) CommunityCategory category,
            @Parameter(
                    description = "정렬 값. 기본값: RECOMMENDED. 가능한 값: RECOMMENDED(추천순), LATEST(최신순)"
            )
            @RequestParam(defaultValue = "RECOMMENDED") CommunityPostSortType sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") Integer size
        ) {
        return CommonResponse.success(communityService.getPosts(category, sort, keyword, cursor, size));
    }

    @GetMapping("/posts/{postId}")
    @Operation(summary = "게시글 상세 조회", description = "게시글 상세 정보(작성자, 이미지, 댓글/답글)를 조회하고 조회수를 증가시킵니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글 없음",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(value = "{\"errorCode\":5001,\"message\":\"게시글을 찾을 수 없습니다.\"}")
                    )
            )
    })
    public CommonResponse<CommunityPostDetailResponseDto> getPostDetail(@PathVariable Long postId) {
        return CommonResponse.success(communityService.getPostDetail(postId));
    }

    @PostMapping("/posts/{postId}/comments")
    @Operation(summary = "댓글/답글 작성", description = "parentCommentId를 비우면 댓글, 넣으면 답글로 작성됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "작성 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 부모 댓글",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = {
                                    @ExampleObject(name = "INVALID_PARENT_COMMENT", value = "{\"errorCode\":5003,\"message\":\"해당 게시글에 속한 댓글만 답글 부모로 지정할 수 있습니다.\"}"),
                                    @ExampleObject(name = "REPLY_DEPTH_NOT_ALLOWED", value = "{\"errorCode\":5004,\"message\":\"답글에는 다시 답글을 작성할 수 없습니다.\"}")
                            }
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
                    description = "게시글/댓글 없음",
                    content = @Content(
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = {
                                    @ExampleObject(name = "POST_NOT_FOUND", value = "{\"errorCode\":5001,\"message\":\"게시글을 찾을 수 없습니다.\"}"),
                                    @ExampleObject(name = "COMMENT_NOT_FOUND", value = "{\"errorCode\":5002,\"message\":\"댓글을 찾을 수 없습니다.\"}")
                            }
                    )
            )
    })
    public CommonResponse<CommunityCommentDto> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommunityCommentCreateRequestDto request
    ) {
        Long userId = extractUserId();
        return CommonResponse.success(communityService.createComment(userId, postId, request));
    }

    @PostMapping("/posts/{postId}/likes")
    @Operation(summary = "게시글 좋아요", description = "게시글에 좋아요를 누릅니다. 이미 좋아요 상태면 유지됩니다.")
    public CommonResponse<CommunityLikeResponseDto> likePost(@PathVariable Long postId) {
        Long userId = extractUserId();
        return CommonResponse.success(communityService.likePost(userId, postId));
    }

    @DeleteMapping("/posts/{postId}/likes")
    @Operation(summary = "게시글 좋아요 취소", description = "게시글 좋아요를 취소합니다. 이미 취소 상태여도 그대로 성공 처리됩니다.")
    public CommonResponse<CommunityLikeResponseDto> unlikePost(@PathVariable Long postId) {
        Long userId = extractUserId();
        return CommonResponse.success(communityService.unlikePost(userId, postId));
    }

    private Long extractUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;

        if (principal instanceof Long userId) {
            return userId;
        }

        if (principal instanceof String userIdText) {
            try {
                return Long.parseLong(userIdText);
            } catch (NumberFormatException ignored) {
            }
        }

        throw ApplicationException.from(AuthErrorCase.UNAUTHORIZED);
    }
}
