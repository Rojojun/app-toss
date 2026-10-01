package com.rojojun.familyshare.item;

import com.rojojun.familyshare.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
public class ItemModel extends BaseEntity {

    private UUID householdId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer lowStockThreshold;
}
