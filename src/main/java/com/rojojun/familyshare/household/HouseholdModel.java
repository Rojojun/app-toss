package com.rojojun.familyshare.household;

import com.rojojun.familyshare.common.ApiException;
import com.rojojun.familyshare.common.BaseEntity;
import com.rojojun.familyshare.common.ErrorCode;
import com.rojojun.familyshare.user.AppUserModel;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Table(name = "household")
public class HouseholdModel extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    @JoinColumn(name = "created_by", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private AppUserModel creator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HouseholdType householdType = HouseholdType.SINGLE;

    public void rename(String name) {
        this.name = name;
    }

    public void dissolve(List<HouseholdMember> members) {
        if (members.size() > 1) {
            throw new ApiException(ErrorCode.HOUSEHOLD_NOT_EMPTY, "다른 멤버가 남아 있어 공간을 해산할 수 없습니다.");
        }

        registerEvent(new HouseholdDissolved(getId()));
    }

    public void changeHouseholdType(HouseholdType householdType, int memberCount) {
        if (this.householdType == householdType) {
            return;
        }

        if (householdType == HouseholdType.SINGLE && memberCount > 1) {
            throw new ApiException(
                    ErrorCode.OWNER_TRANSFER_REQUIRED,
                    "다른 구성원에게 관리 권한을 양도한 뒤 1인가구로 전환해 주세요."
            );
        }

        this.householdType = householdType;
        registerEvent(new HouseholdTypeChanged(getId()));
    }

    public void requireFamily() {
        if (householdType != HouseholdType.FAMILY) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "다인 가구에서만 초대할 수 있습니다. 가구 유형을 먼저 변경해 주세요."
            );
        }
    }

    public void prepareSingleHouseholdDeparture(
            HouseholdMember departing,
            List<HouseholdMember> members,
            UUID newOwnerUserId
    ) {
        requireFamily();
        if (members.size() <= 1) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "혼자 남은 가구는 현재 가구의 유형을 변경해 주세요.");
        }

        if (departing.getRole() == HouseholdMemberRole.OWNER) {
            if (newOwnerUserId == null) {
                throw new ApiException(ErrorCode.OWNER_TRANSFER_REQUIRED,
                        "관리 권한을 양도할 구성원을 선택해 주세요.");
            }
            var target = members.stream()
                    .filter(member -> member.getUserId().equals(newOwnerUserId))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND,
                            "양도할 구성원을 찾을 수 없습니다."));
            departing.transferOwnershipTo(target);
            registerEvent(new HouseholdOwnerDeparted(getId()));
        } else if (newOwnerUserId != null) {
            throw new ApiException(ErrorCode.OWNER_REQUIRED,
                    "일반 구성원은 관리 권한 양도 대상을 지정할 수 없습니다.");
        }

        departing.requireNotOwner();
    }

    public static HouseholdModel of(String name, AppUserModel creator, HouseholdType householdType) {
        HouseholdModel householdModel = new HouseholdModel();

        householdModel.name = name;
        householdModel.creator = creator;
        householdModel.householdType = householdType;

        return householdModel;
    }
}
