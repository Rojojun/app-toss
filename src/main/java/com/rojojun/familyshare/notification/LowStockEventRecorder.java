package com.rojojun.familyshare.notification;

import com.rojojun.familyshare.item.ItemLowStockEntered;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LowStockEventRecorder {

    private final LowStockEventRepository lowStockEventRepository;

    @EventListener
    public void record(ItemLowStockEntered event) {
        lowStockEventRepository.save(LowStockEventModel.of(event.householdId(), event.itemId(), event.itemVersion()));
    }
}
