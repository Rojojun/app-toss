package com.rojojun.familyshare.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutOfStockAlertRepository extends JpaRepository<OutOfStockAlertModel, UUID> {

    @Query("select a from OutOfStockAlertModel a where a.dueAt <= :now order by a.dueAt")
    List<OutOfStockAlertModel> findDue(Instant now);

    @Modifying
    @Query("delete from OutOfStockAlertModel a where a.itemId = :itemId")
    void deleteAllByItemId(UUID itemId);

    @Modifying
    @Query("delete from OutOfStockAlertModel a where a.householdId = :householdId")
    void deleteAllByHouseholdId(UUID householdId);
}
