package com.rojojun.familyshare.notification;

import com.rojojun.familyshare.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 물품이 0이 된 뒤, 채워지지 않은 채 시간이 지나면 구성원에게 보낼 알림의 진행 상태.
 * "언제 보낼지 / 몇 번까지 보낼지"는 이 객체가 정한다.
 */
@Entity
@Table(name = "out_of_stock_alert")
public class OutOfStockAlertModel extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID householdId;

    @Column(nullable = false, updatable = false)
    private UUID itemId;

    @Column(nullable = false, updatable = false)
    private String itemName;

    @Column(nullable = false, updatable = false)
    private UUID actorUserId;

    @Column(nullable = false)
    private Instant dueAt;

    @Column(nullable = false)
    private int remindersSent;

    protected OutOfStockAlertModel() {
    }

    public static OutOfStockAlertModel open(UUID householdId, UUID itemId, String itemName, UUID actorUserId, Instant now, Duration firstDelay) {
        OutOfStockAlertModel alert = new OutOfStockAlertModel();
        alert.householdId = householdId;
        alert.itemId = itemId;
        alert.itemName = itemName;
        alert.actorUserId = actorUserId;
        alert.dueAt = now.plus(firstDelay);
        alert.remindersSent = 0;
        return alert;
    }

    public boolean isDue(Instant now) {
        return !dueAt.isAfter(now);
    }

    /** 보낸 것으로 기록하고 다음 시각을 잡는다. 더 보낼 게 없으면 false(=끝). */
    public boolean recordSent(Instant now, Duration interval, int maxReminders) {
        remindersSent++;
        dueAt = now.plus(interval);
        return remindersSent < maxReminders;
    }

    public UUID getHouseholdId() {
        return householdId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }
}
