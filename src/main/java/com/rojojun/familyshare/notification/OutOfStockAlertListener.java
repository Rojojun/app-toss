package com.rojojun.familyshare.notification;

import com.rojojun.familyshare.item.ItemOutOfStockEntered;
import com.rojojun.familyshare.item.ItemRestocked;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@RequiredArgsConstructor
@Component
public class OutOfStockAlertListener {

    private final OutOfStockAlertRepository outOfStockAlertRepository;
    private final AlertProperties alertProperties;
    private final Clock clock;

    @EventListener
    public void open(ItemOutOfStockEntered event) {
        outOfStockAlertRepository.deleteAllByItemId(event.itemId());
        outOfStockAlertRepository.save(OutOfStockAlertModel.open(
                event.householdId(), event.itemId(), event.itemName(), event.actorUserId(),
                Instant.now(clock), alertProperties.outOfStockDelay()));
    }

    @EventListener
    public void close(ItemRestocked event) {
        outOfStockAlertRepository.deleteAllByItemId(event.itemId());
    }
}
