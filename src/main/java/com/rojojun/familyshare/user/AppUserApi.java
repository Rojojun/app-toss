package com.rojojun.familyshare.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/user")
@RestController
public class AppUserApi {

    private final AppUserService appUserService;

    public AppUserApi(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @PostMapping("/register")
    public ResponseEntity<AppUserDto.Response> register(Long tossUserKey) {
        var result = appUserService.register(tossUserKey);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{tossUserKey}")
    public ResponseEntity<AppUserDto.Response> get(@PathVariable Long tossUserKey) {
        var result = appUserService.get(tossUserKey);
        return ResponseEntity.ok(result);
    }
}
