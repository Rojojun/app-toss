package com.rojojun.familyshare.item;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public class ItemDto {

    public record CreateRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull ItemCategory category,
            @NotBlank @Size(max = 20) String unit,
            @NotNull @Min(0) Integer quantity,
            @NotNull @Min(0) Integer lowStockThreshold
    ) {}

    public record UpdateRequest(
            @NotNull @Min(0) Long expectedVersion,
            @Size(min = 1, max = 100) String name,
            ItemCategory category,
            @Size(min = 1, max = 20) String unit,
            @Min(0) Integer lowStockThreshold
    ) {
        @AssertTrue(message = "변경할 필드가 하나 이상 필요합니다.")
        public boolean isChanged() {
            return name != null || category != null || unit != null || lowStockThreshold != null;
        }

        ItemService.Update toUpdate() {
            return new ItemService.Update(expectedVersion, name, category, unit, lowStockThreshold);
        }
    }

    public record ChangeStockRequest(
            @NotNull @Min(0) Long expectedVersion,
            @NotNull @Min(0) Integer newQuantity
    ) {}

    public record Response(
            UUID id,
            UUID householdId,
            String name,
            ItemCategory category,
            String unit,
            int quantity,
            int lowStockThreshold,
            boolean lowStock,
            long version
    ) {
        static Response from(ItemModel item) {
            return new Response(
                    item.getId(),
                    item.getHouseholdId(),
                    item.getName(),
                    item.getCategory(),
                    item.getUnit(),
                    item.getQuantity(),
                    item.getLowStockThreshold(),
                    item.isLowStock(),
                    item.getVersion()
            );
        }
    }

    public record ListResponse(List<Response> items) {
        static ListResponse from(List<ItemModel> items) {
            return new ListResponse(items.stream().map(Response::from).toList());
        }
    }

    public record StockChangeResponse(
            UUID id,
            UUID itemId,
            UUID actorUserId,
            String actorNickname,
            int beforeQuantity,
            int afterQuantity,
            OffsetDateTime createdAt
    ) {
        static StockChangeResponse from(StockChangeModel stockChange) {
            return new StockChangeResponse(
                    stockChange.getId(),
                    stockChange.getItemId(),
                    stockChange.getActorUserId(),
                    stockChange.getActorNickname(),
                    stockChange.getBeforeQuantity(),
                    stockChange.getAfterQuantity(),
                    stockChange.getCreatedAt().atOffset(ZoneOffset.UTC)
            );
        }
    }

    public record StockChangesResponse(List<StockChangeResponse> stockChanges) {
        static StockChangesResponse from(List<StockChangeModel> stockChanges) {
            return new StockChangesResponse(stockChanges.stream().map(StockChangeResponse::from).toList());
        }
    }

    public record ChangeStockResponse(Response item, StockChangeResponse stockChange) {
        static ChangeStockResponse from(ItemService.StockChanged changed) {
            return new ChangeStockResponse(Response.from(changed.item()), StockChangeResponse.from(changed.stockChange()));
        }
    }
}
