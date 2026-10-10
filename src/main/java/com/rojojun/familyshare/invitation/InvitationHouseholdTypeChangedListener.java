package com.rojojun.familyshare.invitation;

import com.rojojun.familyshare.household.HouseholdTypeChanged;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class InvitationHouseholdTypeChangedListener {

    private final InvitationRepository invitationRepository;

    @EventListener
    public void on(HouseholdTypeChanged event) {
        invitationRepository.deleteAllByHouseholdId(event.uuid());
    }
}
