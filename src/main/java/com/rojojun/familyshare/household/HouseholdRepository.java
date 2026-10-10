package com.rojojun.familyshare.household;

import com.rojojun.familyshare.common.ApiException;
import com.rojojun.familyshare.common.ErrorCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface HouseholdRepository extends JpaRepository<HouseholdModel, UUID> {

    default HouseholdModel getHousehold(UUID householdId) {
        return findById(householdId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "공간을 찾을 수 없습니다."));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HouseholdModel h where h.id = :householdId")
    Optional<HouseholdModel> findByIdForUpdate(@Param("householdId") UUID householdId);

    default HouseholdModel getHouseholdForUpdate(UUID householdId) {
        return findByIdForUpdate(householdId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "공간을 찾을 수 없습니다."));
    }
}
