package com.rojojun.familyshare.item;

import com.rojojun.familyshare.common.ApiException;
import com.rojojun.familyshare.common.ErrorCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<ItemModel, UUID> {

    List<ItemModel> findAllByHouseholdIdOrderByName(UUID householdId);

    Optional<ItemModel> findByIdAndHouseholdId(UUID itemId, UUID householdId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ItemModel i where i.id = :itemId and i.householdId = :householdId")
    Optional<ItemModel> findByIdAndHouseholdIdForUpdate(UUID itemId, UUID householdId);

    default ItemModel getInHousehold(UUID itemId, UUID householdId) {
        return findByIdAndHouseholdId(itemId, householdId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "물품을 찾을 수 없습니다."));
    }

    default ItemModel getInHouseholdForUpdate(UUID itemId, UUID householdId) {
        return findByIdAndHouseholdIdForUpdate(itemId, householdId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "물품을 찾을 수 없습니다."));
    }

    @Modifying
    @Query("delete from ItemModel i where i.householdId = :householdId")
    void deleteAllByHouseholdId(UUID householdId);
}
