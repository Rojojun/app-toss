package com.rojojun.familyshare.item;

import com.rojojun.familyshare.common.CustomPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/households/{householdId}/items")
@RestController
public class ItemApi {

    private final ItemService itemService;

    public ItemApi(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    public ResponseEntity<ItemDto.Response> addItem(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID householdId,
            @RequestBody @Valid ItemDto.CreateRequest request
    ) {
        var item = itemService.create(
                principal.userId(), householdId,
                request.name(), request.category(), request.unit(), request.quantity(), request.lowStockThreshold());
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemDto.Response.from(item));
    }

    @GetMapping
    public ResponseEntity<ItemDto.ListResponse> getItem(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID householdId
    ) {
        var items = itemService.list(principal.userId(), householdId);
        return ResponseEntity.ok(ItemDto.ListResponse.from(items));
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<ItemDto.Response> updateItem(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID householdId,
            @PathVariable UUID itemId,
            @RequestBody @Valid ItemDto.UpdateRequest request
    ) {
        var item = itemService.update(principal.userId(), householdId, itemId, request.toUpdate());
        return ResponseEntity.ok(ItemDto.Response.from(item));
    }

    @PostMapping("/{itemId}/stock-changes")
    public ResponseEntity<ItemDto.ChangeStockResponse> updateItemStock(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID householdId,
            @PathVariable UUID itemId,
            @RequestBody @Valid ItemDto.ChangeStockRequest request
    ) {
        var changed = itemService.changeStock(
                principal.userId(), householdId, itemId, request.expectedVersion(), request.newQuantity());
        return ResponseEntity.ok(ItemDto.ChangeStockResponse.from(changed));
    }

    @GetMapping("/{itemId}/stock-changes")
    public ResponseEntity<ItemDto.StockChangesResponse> getStockChangeHistory(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID householdId,
            @PathVariable UUID itemId
    ) {
        var stockChanges = itemService.stockChanges(principal.userId(), householdId, itemId);
        return ResponseEntity.ok(ItemDto.StockChangesResponse.from(stockChanges));
    }
}
