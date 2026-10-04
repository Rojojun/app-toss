package com.rojojun.familyshare.notification;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class NotificationDto {

    public record PutRequest(@NotNull Boolean enabled) {}

    public record AlertPreferenceResponse(UUID householdId, boolean enabled) {
        static AlertPreferenceResponse from(AlertPreferenceModel preference) {
            return new AlertPreferenceResponse(preference.getHouseholdId(), preference.isEnabled());
        }
    }
}
