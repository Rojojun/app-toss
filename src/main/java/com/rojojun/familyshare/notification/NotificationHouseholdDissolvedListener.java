package com.rojojun.familyshare.notification;

import com.rojojun.familyshare.household.HouseholdDissolved;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class NotificationHouseholdDissolvedListener {

    private final AlertPreferenceRepository alertPreferenceRepository;
    private final LowStockEventRepository lowStockEventRepository;
    private final OutOfStockAlertRepository outOfStockAlertRepository;

    @EventListener
    public void on(HouseholdDissolved event) {
        alertPreferenceRepository.deleteAllByHouseholdId(event.householdId());
        lowStockEventRepository.deleteAllByHouseholdId(event.householdId());
        outOfStockAlertRepository.deleteAllByHouseholdId(event.householdId());
    }
}
