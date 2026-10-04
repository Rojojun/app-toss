package com.rojojun.familyshare.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

public interface AlertPreferenceRepository extends JpaRepository<AlertPreferenceModel, AlertPreferenceId> {

    @Modifying
    @Query("delete from AlertPreferenceModel p where p.id.householdId = :householdId")
    void deleteAllByHouseholdId(UUID householdId);
}
