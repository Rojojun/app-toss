package com.rojojun.familyshare.user;

import java.time.LocalDateTime;
import java.util.UUID;

public class AppUserDto {
    public record Response(
            UUID id,
            Long tossUserKey,
            AppUserStatus appUserStatus,
            LocalDateTime createdAt
    ) {
        Response(AppUserModel appUserModel) {
            this(appUserModel.getId(), appUserModel.getTossUserKey(), appUserModel.getStatus(), appUserModel.getCreatedAt());
        }
    }
}
