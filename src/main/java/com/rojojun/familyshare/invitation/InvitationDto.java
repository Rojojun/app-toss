package com.rojojun.familyshare.invitation;

import com.rojojun.familyshare.household.HouseholdMember;
import com.rojojun.familyshare.household.HouseholdMemberRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class InvitationDto {
    public record IssueRequest(
            @Min(1) @Max(168) int expiresInHours,
            @Min(1) @Max(10) int maxUses
    ) {
        public Duration validFor() {
            return Duration.ofHours(expiresInHours);
        }
    }

    public record IssueResponse(
            UUID id,
            String token,
            Instant expiresAt,
            int maxUses
    ) {
        static IssueResponse from(InvitationService.Issued issued) {
            var invitation = issued.invitation();
            return new IssueResponse(invitation.getId(), issued.rawToken(), invitation.getExpiresAt(), invitation.getMaxUses());
        }
    }

    public record AcceptRequest(@NotBlank @Size(max = 20) String nickname) {}

    public record PreviewResponse(String householdName, boolean available) {
        static PreviewResponse from(InvitationService.Preview preview) {
            return new PreviewResponse(preview.householdName(), preview.available());
        }
    }

    public record AcceptResponse(UUID householdId, HouseholdMemberRole role) {
        static AcceptResponse from(HouseholdMember member) {
            return new AcceptResponse(member.getHouseholdId(), member.getRole());
        }
    }
}
