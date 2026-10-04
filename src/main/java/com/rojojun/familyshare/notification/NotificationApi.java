package com.rojojun.familyshare.notification;

import com.rojojun.familyshare.common.CustomPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class NotificationApi {

    private final NotificationService notificationService;

    public NotificationApi(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/households/{householdId}/alert-preference")
    public ResponseEntity<NotificationDto.AlertPreferenceResponse> getAlertPreference(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID householdId
    ) {
        var preference = notificationService.getAlertPreference(principal.userId(), householdId);
        return ResponseEntity.ok(NotificationDto.AlertPreferenceResponse.from(preference));
    }

    @PutMapping("/households/{householdId}/alert-preference")
    public ResponseEntity<NotificationDto.AlertPreferenceResponse> updateAlertPreference(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID householdId,
            @RequestBody @Valid NotificationDto.PutRequest request
    ) {
        var preference = notificationService.putAlertPreference(principal.userId(), householdId, request.enabled());
        return ResponseEntity.ok(NotificationDto.AlertPreferenceResponse.from(preference));
    }
}
