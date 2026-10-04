package com.rojojun.familyshare.item;

import com.rojojun.familyshare.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "stock_change")
public class StockChangeModel extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID itemId;

    @Column(nullable = false, updatable = false)
    private UUID actorUserId;

    @Column(nullable = false, updatable = false, length = 20)
    private String actorNickname;

    @Column(nullable = false, updatable = false)
    private int beforeQuantity;

    @Column(nullable = false, updatable = false)
    private int afterQuantity;

    protected StockChangeModel() {
    }

    public static StockChangeModel of(UUID itemId, UUID actorUserId, String actorNickname, int beforeQuantity, int afterQuantity) {
        StockChangeModel stockChange = new StockChangeModel();
        stockChange.itemId = itemId;
        stockChange.actorUserId = actorUserId;
        stockChange.actorNickname = actorNickname;
        stockChange.beforeQuantity = beforeQuantity;
        stockChange.afterQuantity = afterQuantity;
        return stockChange;
    }

    public UUID getItemId() {
        return itemId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getActorNickname() {
        return actorNickname;
    }

    public int getBeforeQuantity() {
        return beforeQuantity;
    }

    public int getAfterQuantity() {
        return afterQuantity;
    }
}
