package com.rojojun.familyshare.invitation;

import com.rojojun.familyshare.common.CustomPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class InvitationApi {

    private final InvitationService invitationService;

    public InvitationApi(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/households/{householdId}/invitations")
    public ResponseEntity<InvitationDto.IssueResponse> issue(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID householdId,
            @RequestBody @Valid InvitationDto.IssueRequest request
    ) {
        var issued = invitationService.issue(principal.userId(), householdId, request.maxUses(), request.validFor());
        return ResponseEntity.status(HttpStatus.CREATED).body(InvitationDto.IssueResponse.from(issued));
    }

    @GetMapping("/invitations/{token}")
    public ResponseEntity<InvitationDto.PreviewResponse> preview(@PathVariable String token) {
        return ResponseEntity.ok(InvitationDto.PreviewResponse.from(invitationService.preview(token)));
    }

    @PostMapping("/invitations/{token}/accept")
    public ResponseEntity<InvitationDto.AcceptResponse> accept(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable String token,
            @RequestBody @Valid InvitationDto.AcceptRequest request
    ) {
        var accepted = invitationService.accept(principal.userId(), token, request.nickname());
        return ResponseEntity.status(accepted.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(InvitationDto.AcceptResponse.from(accepted.member()));
    }

    @DeleteMapping("/households/{householdId}/invitations/{invitationId}")
    public ResponseEntity<Void> revoke(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID householdId,
            @PathVariable UUID invitationId
    ) {
        invitationService.revoke(principal.userId(), householdId, invitationId);
        return ResponseEntity.noContent().build();
    }
}
