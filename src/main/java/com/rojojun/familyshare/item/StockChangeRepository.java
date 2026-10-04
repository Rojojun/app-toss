package com.rojojun.familyshare.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.UUID;

public interface StockChangeRepository extends JpaRepository<StockChangeModel, UUID> {

    List<StockChangeModel> findTop50ByItemIdOrderByCreatedAtDescIdDesc(UUID itemId);

    @Modifying
    @Query("delete from StockChangeModel s where s.itemId in (select i.id from ItemModel i where i.householdId = :householdId)")
    void deleteAllByHouseholdId(UUID householdId);
}
