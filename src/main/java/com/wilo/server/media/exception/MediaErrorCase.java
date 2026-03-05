package com.wilo.server.media.exception;

import com.wilo.server.global.exception.ErrorCase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MediaErrorCase implements ErrorCase {

    INVALID_MEDIA_FILE(400, 3403, "미디어 파일이 올바르지 않습니다."),
    UNSUPPORTED_MEDIA_TYPE(400, 3404, "지원하지 않는 미디어 타입입니다."),
    INVALID_SOURCE_TYPE(400, 3405, "sourceType 값이 올바르지 않습니다.");

    private final Integer httpStatusCode;
    private final Integer errorCode;
    private final String message;
}
