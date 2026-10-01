package com.rojojun.familyshare.item;

import com.rojojun.familyshare.common.BaseEntity;
import com.rojojun.familyshare.household.HouseholdModel;
import jakarta.persistence.*;

@Entity
public class ItemModel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id")
    private HouseholdModel householdModel;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer lowStockThreshold;
}
