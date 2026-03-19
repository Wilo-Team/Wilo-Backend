package com.wilo.server.global.exception;

public interface ErrorCase {
    Integer getHttpStatusCode();
    Integer getErrorCode();
    String getMessage();
}
