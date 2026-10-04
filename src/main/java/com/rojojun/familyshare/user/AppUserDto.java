package com.rojojun.familyshare.user;

import com.rojojun.familyshare.household.HouseholdMemberRole;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class AppUserDto {
    public record MyInformationDto(
            User user,
            List<HouseholdSummary> households
    ) { }

    public record User(
            UUID id,
            AppUserStatus appUserStatus,
            OffsetDateTime createdAt
    ) {}

    public record HouseholdSummary(
            UUID id,
            String name,
            HouseholdMemberRole myRole
    ) {}
}
