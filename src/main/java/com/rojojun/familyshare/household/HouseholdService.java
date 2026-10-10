package com.rojojun.familyshare.household;

import com.rojojun.familyshare.invitation.InvitationRepository;
import com.rojojun.familyshare.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class HouseholdService {

    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository memberRepository;
    private final AppUserRepository appUserRepository;
    private final InvitationRepository invitationRepository;

    @Transactional
    public Snapshot create(UUID userId, String name, String nickname, HouseholdType householdType) {
        var user = appUserRepository.getReferenceById(userId);
        var household = householdRepository.save(HouseholdModel.of(name, user, householdType));
        var owner = memberRepository.save(HouseholdMember.owner(household, user, nickname));

        return new Snapshot(household, owner, List.of(owner));
    }

    @Transactional(readOnly = true)
    public Snapshot get(UUID userId, UUID householdId) {
        var me = memberRepository.getMember(householdId, userId);

        return new Snapshot(
                householdRepository.getHousehold(householdId),
                me,
                memberRepository.findAllByIdHouseholdIdOrderByJoinedAt(householdId));
    }

    @Transactional
    public Snapshot rename(UUID userId, UUID householdId, String name) {
        var me = memberRepository.getMember(householdId, userId);
        me.requireOwner();

        var household = householdRepository.getHousehold(householdId);
        household.rename(name);

        return new Snapshot(
                household,
                me,
                memberRepository.findAllByIdHouseholdIdOrderByJoinedAt(householdId));
    }

    @Transactional
    public Snapshot transferOwnership(UUID userId, UUID householdId, UUID newOwnerUserId) {
        var household = householdRepository.getHouseholdForUpdate(householdId);
        var me = memberRepository.getMember(householdId, userId);
        var target = memberRepository.getMember(householdId, newOwnerUserId);

        me.transferOwnershipTo(target);

        return new Snapshot(
                household,
                me,
                memberRepository.findAllByIdHouseholdIdOrderByJoinedAt(householdId));
    }

    @Transactional
    public void dissolve(UUID userId, UUID householdId) {
        var household = householdRepository.getHouseholdForUpdate(householdId);
        memberRepository.getMember(householdId, userId).requireOwner();

        var members = memberRepository.findAllByIdHouseholdIdOrderByJoinedAt(householdId);

        household.dissolve(members);
        memberRepository.deleteAll(members);
        householdRepository.delete(household);
    }

    @Transactional
    public Snapshot changeNickname(UUID userId, UUID householdId, String nickname) {
        var me = memberRepository.getMember(householdId, userId);
        me.changeNickname(nickname);

        return new Snapshot(
                householdRepository.getHousehold(householdId),
                me,
                memberRepository.findAllByIdHouseholdIdOrderByJoinedAt(householdId));
    }

    @Transactional
    public Snapshot changeHouseholdType(UUID userId, UUID householdId, HouseholdType householdType) {
        var household = householdRepository.getHouseholdForUpdate(householdId);
        var me = memberRepository.getMember(householdId, userId);
        me.requireOwner();

        var members = memberRepository.findAllByIdHouseholdIdOrderByJoinedAt(householdId);

        household.changeHouseholdType(householdType, members.size());
        householdRepository.save(household);

        return new Snapshot(household, me, members);
    }

    @Transactional
    public Snapshot startSingleHousehold(
            UUID userId, UUID householdId, UUID newOwnerUserId,
            String name, String nickname
    ) {
        var household = householdRepository.getHouseholdForUpdate(householdId);
        var me = memberRepository.getMember(householdId, userId);
        var members = memberRepository.findAllByIdHouseholdIdOrderByJoinedAt(householdId);

        household.prepareSingleHouseholdDeparture(me, members, newOwnerUserId);
        householdRepository.save(household);
        memberRepository.delete(me);

        return create(userId, name, nickname, HouseholdType.SINGLE);
    }

    @Transactional
    public void leave(UUID userId, UUID householdId) {
        householdRepository.getHouseholdForUpdate(householdId);

        var me = memberRepository.getMember(householdId, userId);
        me.requireNotOwner();

        memberRepository.delete(me);
    }

    @Transactional
    public void removeMember(UUID userId, UUID householdId, UUID targetUserId) {
        householdRepository.getHouseholdForUpdate(householdId);
        memberRepository.getMember(householdId, userId).requireOwner();

        var target = memberRepository.getMember(householdId, targetUserId);
        target.requireNotOwner();

        memberRepository.delete(target);
    }

    public record Snapshot(
            HouseholdModel household,
            HouseholdMember me,
            List<HouseholdMember> members
    ) {}
}
