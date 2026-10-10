package com.rojojun.familyshare.invitation;

import com.rojojun.familyshare.household.HouseholdOwnerDeparted;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class InvitationHouseholdOwnerDepartedListener {

    private final InvitationRepository invitationRepository;

    @EventListener
    public void on(HouseholdOwnerDeparted event) {
        invitationRepository.deleteAllByHouseholdId(event.householdId());
    }
}
