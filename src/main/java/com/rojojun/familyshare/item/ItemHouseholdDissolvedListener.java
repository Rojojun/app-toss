package com.rojojun.familyshare.item;

import com.rojojun.familyshare.household.HouseholdDissolved;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ItemHouseholdDissolvedListener {

    private final StockChangeRepository stockChangeRepository;
    private final ItemRepository itemRepository;

    @EventListener
    public void on(HouseholdDissolved event) {
        stockChangeRepository.deleteAllByHouseholdId(event.householdId());
        itemRepository.deleteAllByHouseholdId(event.householdId());
    }
}
