package com.rojojun.familyshare.invitation;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<InvitationModel, UUID> {
    Optional<InvitationModel> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InvitationModel i where i.tokenHash = :tokenHash")
    Optional<InvitationModel> findByTokenHashForUpdate(String tokenHash);

    default InvitationModel getByTokenHash(String tokenHash) {
        return findByTokenHash(tokenHash).orElseThrow(InvitationNotFoundException::new);
    }

    default InvitationModel getByTokenHashForUpdate(String tokenHash) {
        return findByTokenHashForUpdate(tokenHash).orElseThrow(InvitationNotFoundException::new);
    }

    default InvitationModel getInHousehold(UUID invitationId, UUID householdId) {
        return findById(invitationId)
                .filter(invitation -> invitation.getHouseholdId().equals(householdId))
                .orElseThrow(InvitationNotFoundException::new);
    }

    @Modifying
    @Query("delete from InvitationModel i where i.householdId = :householdId")
    void deleteAllByHouseholdId(UUID householdId);
}
