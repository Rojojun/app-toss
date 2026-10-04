package com.rojojun.familyshare.household;

import com.rojojun.familyshare.common.ApiException;
import com.rojojun.familyshare.common.ErrorCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface HouseholdMemberRepository
        extends JpaRepository<HouseholdMember, HouseholdMemberId> {

    @Query("""
            select h.id as householdId,
                   h.name as householdName,
                   m.role as myRole
            from HouseholdMember m
            join m.householdModel h
            where m.user.id = :userId
            order by h.name
            """)
    List<HouseholdSummaryProjection> findHouseholdsForUser(
            @Param("userId") UUID userId
    );

    List<HouseholdMember> findAllByIdHouseholdIdOrderByJoinedAt(UUID householdId);

    default HouseholdMember getMember(UUID householdId, UUID userId) {
        return findById(new HouseholdMemberId(householdId, userId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "공간을 찾을 수 없습니다."));
    }

    interface HouseholdSummaryProjection {
        UUID getHouseholdId();

        String getHouseholdName();

        HouseholdMemberRole getMyRole();
    }
}