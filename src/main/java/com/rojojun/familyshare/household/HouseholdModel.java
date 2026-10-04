package com.rojojun.familyshare.household;

import com.rojojun.familyshare.common.ApiException;
import com.rojojun.familyshare.common.BaseEntity;
import com.rojojun.familyshare.common.ErrorCode;
import com.rojojun.familyshare.user.AppUserModel;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "household")
public class HouseholdModel extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    @JoinColumn(name = "created_by", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private AppUserModel creator;

    public String getName() {
        return name;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void dissolve(List<HouseholdMember> members) {
        if (members.size() > 1) {
            throw new ApiException(ErrorCode.HOUSEHOLD_NOT_EMPTY, "다른 멤버가 남아 있어 공간을 해산할 수 없습니다.");
        }

        registerEvent(new HouseholdDissolved(getId()));
    }

    public static HouseholdModel of(String name, AppUserModel creator) {
        HouseholdModel householdModel = new HouseholdModel();

        householdModel.name = name;
        householdModel.creator = creator;

        return householdModel;
    }
}
