package com.rojojun.familyshare.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TossAuthApi {

    private final TossAuthService tossAuthService;

    public TossAuthApi(TossAuthService tossAuthService) {
        this.tossAuthService = tossAuthService;
    }

    @PostMapping("/auth/toss/exchange")
    public ResponseEntity<TossAuthDto.AuthResponse> exchange(
            @RequestBody @Valid TossAuthDto.ExchangeRequest request
    ) {
        TossAuthService.ExchangeResult result = tossAuthService.exchange(request);
        HttpStatus status = result.newUserCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }
}
