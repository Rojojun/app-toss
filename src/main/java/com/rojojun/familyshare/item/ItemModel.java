package com.rojojun.familyshare.item;

import com.rojojun.familyshare.common.ApiException;
import com.rojojun.familyshare.common.BaseEntity;
import com.rojojun.familyshare.common.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "item")
public class ItemModel extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID householdId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemCategory category;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int lowStockThreshold;

    @Version
    @Column(nullable = false)
    private long version;


    protected ItemModel() {
    }

    public static ItemModel create(
            UUID householdId,
            String name,
            ItemCategory category,
            String unit,
            int quantity,
            int lowStockThreshold
    ) {
        if (householdId == null) {
            throw new IllegalArgumentException("householdId must not be null");
        }
        if (quantity < 0 || lowStockThreshold < 0) {
            throw new IllegalArgumentException("quantity and lowStockThreshold must not be negative");
        }

        requireValidUnit(unit);

        ItemModel item = new ItemModel();
        item.householdId = householdId;
        item.name = name;
        item.category = category;
        item.unit = unit;
        item.quantity = quantity;
        item.lowStockThreshold = lowStockThreshold;
        return item;
    }

    public void update(long expectedVersion, String name, ItemCategory category, String unit, Integer lowStockThreshold) {
        requireVersion(expectedVersion);
        Optional.ofNullable(unit).ifPresent(ItemModel::requireValidUnit);
        boolean wasLowStock = isLowStock();

        Optional.ofNullable(name).ifPresent(value -> this.name = value);
        Optional.ofNullable(category).ifPresent(value -> this.category = value);
        Optional.ofNullable(unit).ifPresent(value -> this.unit = value);
        Optional.ofNullable(lowStockThreshold).ifPresent(value -> this.lowStockThreshold = value);

        recordLowStockEntry(wasLowStock);
    }

    // 단위는 수량 뒤에 붙는 말(개, 롤…)이다. 숫자가 들어가면 "5" + "1" = "51"처럼 수량으로 읽힌다.
    private static void requireValidUnit(String unit) {
        if (unit != null && unit.chars().anyMatch(Character::isDigit)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "요청 값이 올바르지 않습니다.",
                    Map.of("unit", "단위에는 숫자를 쓸 수 없습니다."));
        }
    }

    public StockChangeModel changeStock(long expectedVersion, int newQuantity, UUID actorUserId, String actorNickname) {
        requireVersion(expectedVersion);

        if (newQuantity == quantity) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "요청 값이 올바르지 않습니다.",
                    Map.of("newQuantity", "현재 수량과 같습니다."));
        }

        boolean wasLowStock = isLowStock();
        int previousQuantity = quantity;
        StockChangeModel stockChange = StockChangeModel.of(getId(), actorUserId, actorNickname, quantity, newQuantity);
        this.quantity = newQuantity;

        recordLowStockEntry(wasLowStock);
        recordOutOfStockTransition(previousQuantity, actorUserId);
        return stockChange;
    }

    public void requireVersion(long expectedVersion) {
        if (version != expectedVersion) {
            throw new ItemVersionConflictException(this);
        }
    }

    void recordLowStockEntry(boolean wasLowStock) {
        if (!wasLowStock && isLowStock()) {
            registerEvent(new ItemLowStockEntered(householdId, getId(), version + 1));
        }
    }

    // 0이 되면 "다 떨어졌어요" 알림 대기를 시작하고, 다시 채워지면(0 초과) 거둔다.
    void recordOutOfStockTransition(int previousQuantity, UUID actorUserId) {
        if (previousQuantity > 0 && quantity == 0) {
            registerEvent(new ItemOutOfStockEntered(householdId, getId(), name, actorUserId));
        }
        if (previousQuantity == 0 && quantity > 0) {
            registerEvent(new ItemRestocked(householdId, getId()));
        }
    }

    public boolean isLowStock() {
        return quantity <= lowStockThreshold;
    }

    public UUID getHouseholdId() {
        return householdId;
    }

    public String getName() {
        return name;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public String getUnit() {
        return unit;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public long getVersion() {
        return version;
    }
}
