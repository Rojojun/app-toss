package com.rojojun.familyshare.auth;

import com.rojojun.familyshare.user.AppUserModel;
import com.rojojun.familyshare.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class UserSessionValidator {

    private final AppUserRepository appUserRepository;

    public boolean isValid(UUID userId) {
        return appUserRepository.findById(userId)
                .filter(AppUserModel::isActive)
                .isPresent();
    }
}
