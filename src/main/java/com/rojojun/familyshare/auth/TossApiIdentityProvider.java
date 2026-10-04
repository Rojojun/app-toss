package com.rojojun.familyshare.auth;

import com.rojojun.familyshare.common.ApiException;
import com.rojojun.familyshare.common.ErrorCode;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class TossApiIdentityProvider implements TossIdentityProvider {

    private static final String EXCHANGE_PATH = "/api-partner/v1/apps-in-toss/users/anon-key/exchange";
    // 4011: 익명 사용자 인증 코드가 유효하지 않음. 그 외 FAIL(4095 한도 초과 등)은 사용자 입력과 무관한 서버 측 오류다.
    private static final String INVALID_CODE_ERROR = "4011";

    private final RestClient tossRestClient;

    public TossApiIdentityProvider(RestClient tossRestClient) {
        this.tossRestClient = tossRestClient;
    }

    @Override
    public VerifiedIdentity verify(String code) {
        ExchangeEnvelope response = tossRestClient.post()
                .uri(EXCHANGE_PATH)
                .body(new ExchangeRequest(code))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, res) -> {
                    throw new ApiException(ErrorCode.TOSS_AUTH_FAILED, "인증 코드가 유효하지 않습니다.");
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, res) -> {
                    throw new ApiException(ErrorCode.TOSS_UPSTREAM_ERROR, "토스 서버 오류입니다.");
                })
                .body(ExchangeEnvelope.class);

        if (response == null) {
            throw new ApiException(ErrorCode.TOSS_UPSTREAM_ERROR, "토스 서버 응답이 비어 있습니다.");
        }
        if (!"SUCCESS".equals(response.resultType())) {
            boolean invalidCode = Optional.ofNullable(response.error())
                    .map(TossError::errorCode)
                    .filter(INVALID_CODE_ERROR::equals)
                    .isPresent();
            throw invalidCode
                    ? new ApiException(ErrorCode.TOSS_AUTH_FAILED, "인증 코드가 유효하지 않습니다.")
                    : new ApiException(ErrorCode.TOSS_UPSTREAM_ERROR, "토스 서버가 요청을 처리하지 못했습니다.");
        }
        if (response.success() == null || response.success().anonKey() == null || response.success().anonKey().isBlank()) {
            throw new ApiException(ErrorCode.TOSS_UPSTREAM_ERROR, "토스 응답에 사용자 식별키가 없습니다.");
        }

        return new VerifiedIdentity(response.success().anonKey());
    }

    private record ExchangeRequest(String code) {
    }

    private record ExchangeEnvelope(String resultType, ExchangePayload success, TossError error) {
    }

    private record ExchangePayload(String anonKey) {
    }

    private record TossError(String errorCode, String reason) {
    }
}
