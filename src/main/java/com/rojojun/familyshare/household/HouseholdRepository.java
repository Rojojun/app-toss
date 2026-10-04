package com.rojojun.familyshare.household;

import com.rojojun.familyshare.common.ApiException;
import com.rojojun.familyshare.common.ErrorCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HouseholdRepository extends JpaRepository<HouseholdModel, UUID> {

    default HouseholdModel getHousehold(UUID householdId) {
        return findById(householdId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "공간을 찾을 수 없습니다."));
    }
}
