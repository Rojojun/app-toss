package com.rojojun.familyshare.household;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public record HouseholdMemberId(
        @Column(name = "household_id") UUID householdId,
        @Column(name = "user_id") UUID userId
) { }
