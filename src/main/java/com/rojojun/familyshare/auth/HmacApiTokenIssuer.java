package com.rojojun.familyshare.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Component
public class HmacApiTokenIssuer implements ApiTokenIssuer {

    private static final String JWT_HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String secretBase64;
    private final long expiresInSeconds;

    public HmacApiTokenIssuer(
            @Value("${family-share.auth.jwt.secret-base64:}") String secretBase64,
            @Value("${family-share.auth.jwt.expires-in-seconds:3600}") long expiresInSeconds
    ) {
        this.secretBase64 = secretBase64;
        this.expiresInSeconds = expiresInSeconds;
    }

    @Override
    public IssuedToken issue(UUID appUserId) {
        byte[] secret = decodeAndValidateSecret();
        if (expiresInSeconds <= 0) {
            throw new IllegalStateException("JWT 만료 시간은 양수여야 합니다.");
        }

        Instant now = Instant.now();
        long issuedAt = now.getEpochSecond();
        long expiresAt = now.plusSeconds(expiresInSeconds).getEpochSecond();

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String encodedHeader = encoder.encodeToString(JWT_HEADER.getBytes(StandardCharsets.UTF_8));
        String payload = "{\"sub\":\"" + appUserId + "\",\"iat\":" + issuedAt + ",\"exp\":" + expiresAt + "}";
        String encodedPayload = encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signingInput = encodedHeader + "." + encodedPayload;

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            String signature = encoder.encodeToString(
                    mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII))
            );
            return new IssuedToken(
                    signingInput + "." + signature,
                    expiresInSeconds
            );
        } catch (Exception exception) {
            throw new IllegalStateException("API 토큰을 발급하지 못했습니다.", exception);
        }
    }

    private byte[] decodeAndValidateSecret() {
        try {
            byte[] secret = Base64.getDecoder().decode(secretBase64);
            if (secret.length < 32) {
                throw new IllegalStateException("JWT 서명 키는 Base64 디코딩 후 32바이트 이상이어야 합니다.");
            }
            return secret;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT 서명 키는 Base64 문자열이어야 합니다.", exception);
        }
    }
}
