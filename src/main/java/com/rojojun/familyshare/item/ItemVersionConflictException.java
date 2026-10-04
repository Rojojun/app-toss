package com.rojojun.familyshare.item;

import com.rojojun.familyshare.common.ApiException;
import com.rojojun.familyshare.common.ErrorCode;

public class ItemVersionConflictException extends ApiException {

    private final transient ItemModel currentItem;

    public ItemVersionConflictException(ItemModel currentItem) {
        super(ErrorCode.VERSION_CONFLICT, "다른 가족이 먼저 물품을 변경했습니다.");
        this.currentItem = currentItem;
    }

    public ItemModel currentItem() {
        return currentItem;
    }
}
