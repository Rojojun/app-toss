package com.rojojun.familyshare.item;

import java.util.UUID;

public record ItemOutOfStockEntered(UUID householdId, UUID itemId, String itemName, UUID actorUserId) {}
