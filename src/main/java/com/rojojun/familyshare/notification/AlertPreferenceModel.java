package com.rojojun.familyshare.notification;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "alert_preference")
public class AlertPreferenceModel {

    @EmbeddedId
    private AlertPreferenceId id;

    @Column(nullable = false)
    private boolean enabled;

    protected AlertPreferenceModel() {
    }

    public static AlertPreferenceModel of(UUID householdId, UUID userId, boolean enabled) {
        AlertPreferenceModel preference = new AlertPreferenceModel();
        preference.id = new AlertPreferenceId(householdId, userId);
        preference.enabled = enabled;
        return preference;
    }

    public UUID getHouseholdId() {
        return id.householdId();
    }

    public boolean isEnabled() {
        return enabled;
    }
}
