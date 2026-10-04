package com.rojojun.familyshare.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.util.UUID;

public interface LowStockEventRepository extends JpaRepository<LowStockEventModel, UUID> {

    @Modifying
    @Query("delete from LowStockEventModel e where e.householdId = :householdId")
    void deleteAllByHouseholdId(UUID householdId);
}
