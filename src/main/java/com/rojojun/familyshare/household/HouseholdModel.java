package com.rojojun.familyshare.household;

import com.rojojun.familyshare.common.BaseEntity;
import com.rojojun.familyshare.user.AppUserModel;
import jakarta.persistence.*;

@Entity
@Table(name = "household")
public class HouseholdModel extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    @JoinColumn(name = "created_by", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private AppUserModel creator;

    public static HouseholdModel of(String name, AppUserModel creator) {
        HouseholdModel householdModel = new HouseholdModel();

        householdModel.name = name;
        householdModel.creator = creator;

        return householdModel;
    }
}
