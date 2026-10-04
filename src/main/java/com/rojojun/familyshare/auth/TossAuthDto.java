package com.rojojun.familyshare.auth;

import com.rojojun.familyshare.user.AppUserModel;
import com.rojojun.familyshare.user.AppUserStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public class TossAuthDto {
    public record ExchangeRequest(
            @NotBlank String code
    ) {
    }

    public record AuthResponse(
            String accessToken,
            String tokenType,
            long expiresInSeconds,
            UserResponse user
    ) {
    }

    public record UserResponse(
            UUID id,
            AppUserStatus status,
            OffsetDateTime createdAt
    ) {
        public static UserResponse from(AppUserModel user) {
            return new UserResponse(
                    user.getId(),
                    user.getStatus(),
                    user.getCreatedAt().atOffset(ZoneOffset.UTC)
            );
        }
    }
}
