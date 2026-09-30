package com.rojojun.familyshare.household;

import com.rojojun.familyshare.common.BaseEntity;
import com.rojojun.familyshare.user.AppUserModel;
import jakarta.persistence.*;

@Entity
@Table(name = "household")
public class Household extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    @JoinColumn(name = "created_by", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private AppUserModel creator;

    public static Household of(String name, AppUserModel creator) {
        Household household = new Household();

        household.name = name;
        household.creator = creator;

        return household;
    }
}
