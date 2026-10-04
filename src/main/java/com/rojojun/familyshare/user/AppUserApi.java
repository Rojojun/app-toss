package com.rojojun.familyshare.user;

import com.rojojun.familyshare.common.CustomPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class AppUserApi {

    private final AppUserService appUserService;

    public AppUserApi(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping("/me")
    public ResponseEntity<AppUserDto.MyInformationDto> getMyInformation(@AuthenticationPrincipal CustomPrincipal customPrincipal) {
        var response = appUserService.getMyInformation(customPrincipal.userId());
        return ResponseEntity.ok(response);
    }
}
