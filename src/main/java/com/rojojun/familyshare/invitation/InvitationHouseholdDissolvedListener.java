package com.rojojun.familyshare.invitation;

import com.rojojun.familyshare.household.HouseholdDissolved;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class InvitationHouseholdDissolvedListener {

    private final InvitationRepository invitationRepository;

    @EventListener
    public void on(HouseholdDissolved event) {
        invitationRepository.deleteAllByHouseholdId(event.householdId());
    }
}
