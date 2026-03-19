package com.wilo.server.notification.error;

import com.wilo.server.global.exception.ErrorCase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCase implements ErrorCase {

    NOTIFICATION_NOT_FOUND(404, 7001, "알림을 찾을 수 없습니다."),
    FORBIDDEN_NOTIFICATION_ACCESS(403, 7002, "본인의 알림만 처리할 수 있습니다.");

    private final Integer httpStatusCode;
    private final Integer errorCode;
    private final String message;
}
