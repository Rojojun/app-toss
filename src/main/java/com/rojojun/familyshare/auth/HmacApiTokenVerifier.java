package com.rojojun.familyshare.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Component
public class HmacApiTokenVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String JWT_ALGORITHM = "HS256";

    private final byte[] secret;
    private final ObjectMapper objectMapper;

    public HmacApiTokenVerifier(
            @Value("${family-share.auth.jwt.secret-base64:}") String secretBase64,
            ObjectMapper objectMapper
    ) {
        this.secret = decodeSecret(secretBase64);
        this.objectMapper = objectMapper;
    }

    public UUID verifyAndGetUserId(String token) {
        return parse(token)
                .filter(this::hasValidSignature)
                .flatMap(this::parseClaims)
                .filter(this::isNotExpired)
                .flatMap(this::subject)
                .orElseThrow(() -> new IllegalArgumentException("API 토큰이 유효하지 않습니다."));
    }

    private Optional<ParsedToken> parse(String token) {
        return Optional.ofNullable(token)
                .filter(value -> !value.isBlank())
                .map(value -> value.split("\\.", -1))
                .filter(parts -> parts.length == 3)
                .flatMap(this::decodeParts);
    }

    private Optional<ParsedToken> decodeParts(String[] parts) {
        try {
            Base64.Decoder decoder = Base64.getUrlDecoder();

            return Optional.of(new ParsedToken(
                    decoder.decode(parts[0]),
                    decoder.decode(parts[1]),
                    decoder.decode(parts[2]),
                    parts[0] + "." + parts[1]
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private boolean hasValidSignature(ParsedToken token) {
        byte[] expectedSignature = sign(token.signingInput());
        return MessageDigest.isEqual(expectedSignature, token.signature());
    }

    private byte[] sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("JWT 서명을 검증할 수 없습니다.", exception);
        }
    }

    private Optional<VerifiedToken> parseClaims(ParsedToken token) {
        return readJson(token.header())
                .filter(header -> JWT_ALGORITHM.equals(header.path("alg").asString()))
                .flatMap(ignored -> readJson(token.payload()))
                .map(VerifiedToken::new);
    }

    private Optional<JsonNode> readJson(byte[] json) {
        try {
            return Optional.ofNullable(objectMapper.readTree(json));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private boolean isNotExpired(VerifiedToken token) {
        JsonNode expiresAt = token.claims().path("exp");
        return expiresAt.isIntegralNumber()
                && expiresAt.asLong() > Instant.now().getEpochSecond();
    }

    private Optional<UUID> subject(VerifiedToken token) {
        return Optional.of(token.claims().path("sub"))
                .filter(JsonNode::isString)
                .map(JsonNode::asString)
                .flatMap(this::parseUserId);
    }

    private Optional<UUID> parseUserId(String userId) {
        try {
            return Optional.of(UUID.fromString(userId));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static byte[] decodeSecret(String secretBase64) {
        byte[] decodedSecret = Base64.getDecoder().decode(secretBase64);
        return Optional.of(decodedSecret)
                .filter(value -> value.length >= 32)
                .orElseThrow(() -> new IllegalStateException(
                        "JWT 서명 키는 Base64 디코딩 후 32바이트 이상이어야 합니다."
                ));
    }

    private record ParsedToken(
            byte[] header,
            byte[] payload,
            byte[] signature,
            String signingInput
    ) {}

    private record VerifiedToken(JsonNode claims) {}
}
