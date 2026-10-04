package com.rojojun.familyshare.household;

import com.rojojun.familyshare.common.CustomPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/households")
@RestController
public class HouseholdApi {

    private final HouseholdService householdService;

    public HouseholdApi(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @PostMapping
    public ResponseEntity<HouseholdDto.Detail> createHousehold(
            @AuthenticationPrincipal CustomPrincipal principal,
            @RequestBody @Valid HouseholdDto.CreateRequest request
    ) {
        var snapshot = householdService.create(principal.userId(), request.name(), request.nickname());
        return ResponseEntity.status(HttpStatus.CREATED).body(HouseholdDto.Detail.from(snapshot));
    }

    @GetMapping("/{householdId}")
    public ResponseEntity<HouseholdDto.Detail> getHousehold(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable("householdId") UUID householdId
    ) {
        var snapshot = householdService.get(principal.userId(), householdId);
        return ResponseEntity.ok(HouseholdDto.Detail.from(snapshot));
    }

    @PatchMapping("/{householdId}")
    public ResponseEntity<HouseholdDto.Detail> updateHousehold(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable("householdId") UUID householdId,
            @RequestBody @Valid HouseholdDto.NameRequest request
    ) {
        var snapshot = householdService.rename(principal.userId(), householdId, request.name());
        return ResponseEntity.ok(HouseholdDto.Detail.from(snapshot));
    }

    @PostMapping("/{householdId}/ownership-transfer")
    public ResponseEntity<HouseholdDto.Detail> transferOwnership(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID householdId,
            @RequestBody @Valid HouseholdDto.TransferRequest request
    ) {
        var snapshot = householdService.transferOwnership(principal.userId(), householdId, request.newOwnerUserId());
        return ResponseEntity.ok(HouseholdDto.Detail.from(snapshot));
    }

    @DeleteMapping("/{householdId}")
    public ResponseEntity<Void> dissolveHousehold(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID householdId
    ) {
        householdService.dissolve(principal.userId(), householdId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{householdId}/members/{userId}")
    public ResponseEntity<Void> deleteHouseholdMembers(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID householdId,
            @PathVariable UUID userId
    ) {
        householdService.removeMember(principal.userId(), householdId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{householdId}/membership")
    public ResponseEntity<HouseholdDto.Detail> changeNickname(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID householdId,
            @RequestBody @Valid HouseholdDto.NicknameRequest request
    ) {
        var snapshot = householdService.changeNickname(principal.userId(), householdId, request.nickname());
        return ResponseEntity.ok(HouseholdDto.Detail.from(snapshot));
    }

    @DeleteMapping("/{householdId}/membership")
    public ResponseEntity<Void> leaveHousehold(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID householdId
    ) {
        householdService.leave(principal.userId(), householdId);
        return ResponseEntity.noContent().build();
    }
}
