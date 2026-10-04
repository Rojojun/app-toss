package com.rojojun.familyshare.invitation;

import com.rojojun.familyshare.household.HouseholdMember;
import com.rojojun.familyshare.household.HouseholdMemberId;
import com.rojojun.familyshare.household.HouseholdMemberRepository;
import com.rojojun.familyshare.household.HouseholdRepository;
import com.rojojun.familyshare.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final InvitationTokenGenerator tokenGenerator;
    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository memberRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public Issued issue(UUID userId, UUID householdId, int maxUses, Duration validFor) {
        memberRepository.getMember(householdId, userId).requireOwner();

        var issued = tokenGenerator.generate();
        var invitation = invitationRepository.save(InvitationModel.create(
                householdId,
                issued.tokenHash(),
                maxUses,
                Instant.now().plus(validFor)
        ));

        return new Issued(invitation, issued.rawToken());
    }

    @Transactional(readOnly = true)
    public Preview preview(String rawToken) {
        var invitation = invitationRepository.getByTokenHash(tokenGenerator.hash(rawToken));
        var household = householdRepository.getHousehold(invitation.getHouseholdId());

        return new Preview(household.getName(), invitation.isAvailableAt(Instant.now()));
    }

    @Transactional
    public Accepted accept(UUID userId, String rawToken, String nickname) {
        var invitation = invitationRepository.getByTokenHashForUpdate(tokenGenerator.hash(rawToken));

        return memberRepository.findById(new HouseholdMemberId(invitation.getHouseholdId(), userId))
                .map(existing -> new Accepted(existing, false))
                .orElseGet(() -> new Accepted(memberRepository.save(invitation.accept(
                        householdRepository.getReferenceById(invitation.getHouseholdId()),
                        appUserRepository.getReferenceById(userId),
                        nickname,
                        Instant.now())), true));
    }

    @Transactional
    public void revoke(UUID userId, UUID householdId, UUID invitationId) {
        memberRepository.getMember(householdId, userId).requireOwner();
        invitationRepository.getInHousehold(invitationId, householdId).revokeAt(Instant.now());
    }

    public record Issued(InvitationModel invitation, String rawToken) {}

    public record Preview(String householdName, boolean available) {}

    public record Accepted(HouseholdMember member, boolean created) {}
}
