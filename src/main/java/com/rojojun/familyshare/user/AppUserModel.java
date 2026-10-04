package com.rojojun.familyshare.user;

import com.rojojun.familyshare.common.ApiException;
import com.rojojun.familyshare.common.BaseEntity;
import com.rojojun.familyshare.common.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity(name = "app_user")
public class AppUserModel extends BaseEntity {
    @Column(name = "익명 유저 키", nullable = false, unique = true, updatable = false)
    private String anonKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "유저 상태", nullable = false)
    private AppUserStatus status;

    public String getAnonKey() {
        return anonKey;
    }

    public AppUserStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == AppUserStatus.ACTIVE;
    }

    public void requireActive() {
        if (!isActive()) {
            throw new ApiException(ErrorCode.USER_INACTIVE, "사용할 수 없는 사용자입니다.");
        }
    }

    public static AppUserModel of(String anonKey) {
        if (anonKey == null || anonKey.isBlank()) {
            throw new IllegalArgumentException("anonKey must not be blank");
        }

        AppUserModel model = new AppUserModel();

        model.anonKey = anonKey;
        model.status = AppUserStatus.ACTIVE;

        return model;
    }
}
