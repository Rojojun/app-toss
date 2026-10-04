package com.rojojun.familyshare.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    TOSS_AUTH_FAILED(HttpStatus.UNAUTHORIZED),
    USER_INACTIVE(HttpStatus.FORBIDDEN),
    OWNER_REQUIRED(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND),
    INVITATION_UNAVAILABLE(HttpStatus.GONE),
    VERSION_CONFLICT(HttpStatus.CONFLICT),
    OWNER_TRANSFER_REQUIRED(HttpStatus.CONFLICT),
    HOUSEHOLD_NOT_EMPTY(HttpStatus.CONFLICT),
    // 502/504는 Cloudflare 같은 프록시가 본문을 자체 오류 페이지로 바꿔서 브라우저에서 CORS 오류처럼 보이므로 503을 쓴다.
    TOSS_UPSTREAM_ERROR(HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
