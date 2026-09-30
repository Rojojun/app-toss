package com.rojojun.familyshare.user;

import com.rojojun.familyshare.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity(name = "app_user")
public class AppUserModel extends BaseEntity {
    @Column(name = "토스 유저 키")
    private Long tossUserKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "유저 상태")
    private AppUserStatus status;

    public Long getTossUserKey() {
        return tossUserKey;
    }

    public AppUserStatus getStatus() {
        return status;
    }

    public static AppUserModel of(Long tossUserKey) {
        AppUserModel model = new AppUserModel();

        model.tossUserKey = tossUserKey;
        model.status = AppUserStatus.ACTIVE;

        return model;
    }
}
