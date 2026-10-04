package com.rojojun.familyshare.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    public record ErrorResponse(String code, String message, Map<String, Object> details) {
        static ErrorResponse of(ErrorCode code, String message, Map<String, Object> details) {
            return new ErrorResponse(code.name(), message, details);
        }
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        return ResponseEntity.status(e.code().status())
                .body(ErrorResponse.of(e.code(), e.getMessage(), e.details()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, Object> fields = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> String.valueOf(error.getDefaultMessage()),
                        (first, second) -> first));
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "요청 값이 올바르지 않습니다.", fields));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> handleBadParameter(Exception e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "요청 파라미터가 올바르지 않습니다.", Map.of()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "요청 본문을 읽을 수 없습니다.", Map.of()));
    }

    @ExceptionHandler(RestClientException.class)
    ResponseEntity<ErrorResponse> handleUpstream(RestClientException e) {
        log.warn("토스 서버 통신 실패", e);
        return ResponseEntity.status(ErrorCode.TOSS_UPSTREAM_ERROR.status())
                .body(ErrorResponse.of(ErrorCode.TOSS_UPSTREAM_ERROR, "토스 서버와 통신하지 못했습니다.", Map.of()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, "서버 오류가 발생했습니다.", Map.of()));
    }
}
