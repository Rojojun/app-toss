package com.rojojun.familyshare.item;

import java.util.UUID;

public record ItemLowStockEntered(UUID householdId, UUID itemId, long itemVersion) {}
