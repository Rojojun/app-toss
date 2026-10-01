package com.rojojun.familyshare.household;

import com.rojojun.familyshare.user.AppUserModel;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "household_member")
public class HouseholdMember {
    @EmbeddedId
    private HouseholdMemberId id;

    @MapsId("householdId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id")
    private HouseholdModel householdModel;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUserModel user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HouseholdMemberRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected HouseholdMember() {
    }

    public static HouseholdMember owner(HouseholdModel householdModel, AppUserModel user) {
        HouseholdMember member = new HouseholdMember();
        member.id = new HouseholdMemberId(householdModel.getId(), user.getId());
        member.householdModel = householdModel;
        member.user = user;
        member.role = HouseholdMemberRole.OWNER;
        member.joinedAt = Instant.now();
        return member;
    }
}
