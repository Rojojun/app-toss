package com.rojojun.familyshare.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record AlertPreferenceId(
        @Column(name = "household_id") UUID householdId,
        @Column(name = "user_id") UUID userId
) {}
