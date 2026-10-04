package com.rojojun.familyshare.item;

import com.rojojun.familyshare.household.HouseholdMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final StockChangeRepository stockChangeRepository;
    private final HouseholdMemberRepository memberRepository;

    @Transactional
    public ItemModel create(
            UUID userId,
            UUID householdId,
            String name,
            ItemCategory category,
            String unit,
            int quantity,
            int lowStockThreshold
    ) {
        memberRepository.getMember(householdId, userId);

        return itemRepository.save(
                ItemModel.create(householdId, name, category, unit, quantity, lowStockThreshold));
    }

    @Transactional(readOnly = true)
    public List<ItemModel> list(UUID userId, UUID householdId) {
        memberRepository.getMember(householdId, userId);

        return itemRepository.findAllByHouseholdIdOrderByName(householdId);
    }

    @Transactional
    public ItemModel update(UUID userId, UUID householdId, UUID itemId, Update update) {
        memberRepository.getMember(householdId, userId);

        var item = itemRepository.getInHouseholdForUpdate(itemId, householdId);
        item.update(update.expectedVersion(), update.name(), update.category(), update.unit(), update.lowStockThreshold());

        return itemRepository.save(item);
    }

    @Transactional
    public StockChanged changeStock(UUID userId, UUID householdId, UUID itemId, long expectedVersion, int newQuantity) {
        var member = memberRepository.getMember(householdId, userId);

        var item = itemRepository.getInHouseholdForUpdate(itemId, householdId);
        var stockChange = stockChangeRepository.save(item.changeStock(expectedVersion, newQuantity, userId, member.getNickname()));

        return new StockChanged(itemRepository.save(item), stockChange);
    }

    @Transactional(readOnly = true)
    public List<StockChangeModel> stockChanges(UUID userId, UUID householdId, UUID itemId) {
        memberRepository.getMember(householdId, userId);
        itemRepository.getInHousehold(itemId, householdId);

        return stockChangeRepository.findTop50ByItemIdOrderByCreatedAtDescIdDesc(itemId);
    }

    public record Update(long expectedVersion, String name, ItemCategory category, String unit, Integer lowStockThreshold) {}

    public record StockChanged(ItemModel item, StockChangeModel stockChange) {}
}
