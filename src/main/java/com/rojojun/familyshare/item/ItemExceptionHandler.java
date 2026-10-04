package com.rojojun.familyshare.item;

import com.rojojun.familyshare.common.ApiExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = ItemApi.class)
public class ItemExceptionHandler {

    @ExceptionHandler(ItemVersionConflictException.class)
    ResponseEntity<ApiExceptionHandler.ErrorResponse> handleVersionConflict(ItemVersionConflictException e) {
        return ResponseEntity.status(e.code().status())
                .body(new ApiExceptionHandler.ErrorResponse(
                        e.code().name(),
                        e.getMessage(),
                        Map.of("currentItem", ItemDto.Response.from(e.currentItem()))));
    }
}
