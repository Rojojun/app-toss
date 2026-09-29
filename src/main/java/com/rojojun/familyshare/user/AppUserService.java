package com.rojojun.familyshare.user;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;

    public AppUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUserDto.Response register(Long tossUserKey) {
        AppUserModel appUserModel = appUserRepository.findByTossUserKey(tossUserKey)
                .orElseGet(() -> appUserRepository.save(AppUserModel.of(tossUserKey)));

        return new AppUserDto.Response(appUserModel);
    }

    public AppUserDto.Response get(Long tossUserKey) {
        AppUserModel result = appUserRepository.findByTossUserKey(tossUserKey)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 유저입니다."));
        return new AppUserDto.Response(result);
    }
}
