package com.rojojun.familyshare.household;

import com.rojojun.familyshare.common.ApiException;
import com.rojojun.familyshare.common.ErrorCode;
import com.rojojun.familyshare.user.AppUserModel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HouseholdMemberRole role;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    public UUID getHouseholdId() {
        return id.householdId();
    }

    public UUID getUserId() {
        return id.userId();
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public String getNickname() {
        return nickname;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void requireNotOwner() {
        if (role == HouseholdMemberRole.OWNER) {
            throw new ApiException(ErrorCode.OWNER_TRANSFER_REQUIRED, "소유권을 이전하거나 공간을 해산한 뒤에 처리할 수 있습니다.");
        }
    }

    public void transferOwnershipTo(HouseholdMember target) {
        requireOwner();

        if (target.getUserId().equals(getUserId())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "요청 값이 올바르지 않습니다.",
                    Map.of("newOwnerUserId", "본인에게는 소유권을 이전할 수 없습니다."));
        }

        target.role = HouseholdMemberRole.OWNER;
        this.role = HouseholdMemberRole.MEMBER;
    }

    public void requireOwner() {
        if (role != HouseholdMemberRole.OWNER) {
            throw new ApiException(ErrorCode.OWNER_REQUIRED, "소유자만 사용할 수 있는 기능입니다.");
        }
    }

    public static HouseholdMember member(HouseholdModel householdModel, AppUserModel user, String nickname) {
        HouseholdMember member = new HouseholdMember();
        member.id = new HouseholdMemberId(householdModel.getId(), user.getId());
        member.householdModel = householdModel;
        member.user = user;
        member.role = HouseholdMemberRole.MEMBER;
        member.nickname = nickname;
        member.joinedAt = Instant.now();
        return member;
    }

    public static HouseholdMember owner(HouseholdModel householdModel, AppUserModel user, String nickname) {
        HouseholdMember member = new HouseholdMember();
        member.id = new HouseholdMemberId(householdModel.getId(), user.getId());
        member.householdModel = householdModel;
        member.user = user;
        member.role = HouseholdMemberRole.OWNER;
        member.nickname = nickname;
        member.joinedAt = Instant.now();
        return member;
    }
}
