package com.rojojun.familyshare.invitation;

import com.rojojun.familyshare.common.ApiException;
import com.rojojun.familyshare.common.BaseEntity;
import com.rojojun.familyshare.common.ErrorCode;
import com.rojojun.familyshare.household.HouseholdMember;
import com.rojojun.familyshare.household.HouseholdModel;
import com.rojojun.familyshare.user.AppUserModel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "invitation")
public class InvitationModel extends BaseEntity {

    @Column(nullable = false)
    private UUID householdId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int maxUses;

    @Column(nullable = false)
    private int usedCount;

    private Instant revokedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public static InvitationModel create(
            UUID householdId,
            String tokenHash,
            int maxUses,
            Instant expiresAt
    ) {
        if (householdId == null) {
            throw new IllegalArgumentException("householdId must not be null");
        }
        if (tokenHash == null || !tokenHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("tokenHash must be a SHA-256 hex digest");
        }
        if (maxUses < 1 || maxUses > 10) {
            throw new IllegalArgumentException("maxUses must be between 1 and 10");
        }
        if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("expiresAt must be in the future");
        }

        InvitationModel invitation = new InvitationModel();
        invitation.householdId = householdId;
        invitation.tokenHash = tokenHash;
        invitation.maxUses = maxUses;
        invitation.expiresAt = expiresAt;
        invitation.usedCount = 0;
        return invitation;
    }

    public boolean isAvailableAt(Instant now) {
        return revokedAt == null
                && expiresAt.isAfter(now)
                && usedCount < maxUses;
    }

    public HouseholdMember accept(HouseholdModel household, AppUserModel user, String nickname, Instant now) {
        if (!isAvailableAt(now)) {
            throw new ApiException(ErrorCode.INVITATION_UNAVAILABLE, "사용할 수 없는 초대입니다.");
        }
        consumeUseAt(now);
        return HouseholdMember.member(household, user, nickname);
    }

    public void consumeUseAt(Instant now) {
        if (!isAvailableAt(now)) {
            throw new IllegalStateException("Invitation is not available");
        }
        usedCount++;
    }

    public void revokeAt(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }
}
