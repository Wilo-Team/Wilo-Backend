package com.wilo.server.community.error;

import com.wilo.server.global.exception.ErrorCase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommunityErrorCase implements ErrorCase {

    POST_NOT_FOUND(404, 5001, "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(404, 5002, "댓글을 찾을 수 없습니다."),
    INVALID_PARENT_COMMENT(400, 5003, "해당 게시글에 속한 댓글만 답글 부모로 지정할 수 있습니다.");

    private final Integer httpStatusCode;
    private final Integer errorCode;
    private final String message;
}
