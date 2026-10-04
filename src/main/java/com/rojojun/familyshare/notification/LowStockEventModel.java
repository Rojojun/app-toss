package com.rojojun.familyshare.notification;

import com.rojojun.familyshare.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "low_stock_event")
public class LowStockEventModel extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID householdId;

    @Column(nullable = false, updatable = false)
    private UUID itemId;

    @Column(nullable = false, updatable = false)
    private long itemVersion;

    protected LowStockEventModel() {
    }

    public static LowStockEventModel of(UUID householdId, UUID itemId, long itemVersion) {
        LowStockEventModel event = new LowStockEventModel();
        event.householdId = householdId;
        event.itemId = itemId;
        event.itemVersion = itemVersion;
        return event;
    }

    public UUID getItemId() {
        return itemId;
    }

    public long getItemVersion() {
        return itemVersion;
    }
}
