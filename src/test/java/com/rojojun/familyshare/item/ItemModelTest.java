package com.rojojun.familyshare.item;

import com.rojojun.familyshare.notification.OutOfStockAlertRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ItemModelTest {

    @Autowired private ItemRepository itemRepository;
    @Autowired private OutOfStockAlertRepository  outOfStockAlertRepository;

    @DisplayName("수량이 최소 기준 점이 되면 알람 대기 행을 저장한다")
    @Transactional
    @Test
    void eventStart1() {
        var item = itemRepository.save(ItemModel.create(
                UUID.randomUUID(),
                "치약",
                ItemCategory.TOILETRIES,
                "개",
                4,
                3
        ));
        var actorId = UUID.randomUUID();
        item.changeStock(0,  3, actorId, "테스트 사용자");
        itemRepository.save(item);

        assertThat(outOfStockAlertRepository.findAll())
                .anyMatch(alert -> alert.getItemId().equals(item.getId()));
    }
}