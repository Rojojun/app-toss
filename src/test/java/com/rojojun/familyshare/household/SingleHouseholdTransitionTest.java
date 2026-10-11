package com.rojojun.familyshare.household;

import com.rojojun.familyshare.common.ApiException;
import com.rojojun.familyshare.invitation.InvitationModel;
import com.rojojun.familyshare.invitation.InvitationRepository;
import com.rojojun.familyshare.item.ItemCategory;
import com.rojojun.familyshare.item.ItemModel;
import com.rojojun.familyshare.item.ItemRepository;
import com.rojojun.familyshare.user.AppUserModel;
import com.rojojun.familyshare.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class SingleHouseholdTransitionTest {
    @Autowired HouseholdService service;
    @Autowired HouseholdRepository households;
    @Autowired HouseholdMemberRepository members;
    @Autowired AppUserRepository users;
    @Autowired InvitationRepository invitations;
    @Autowired ItemRepository items;
    @Autowired PlatformTransactionManager transactionManager;

    private TransactionTemplate tx() {
        return new TransactionTemplate(transactionManager);
    }

    private Fixture fixture(boolean withMember) {
        return tx().execute(status -> {
            var owner = users.save(AppUserModel.of(UUID.randomUUID().toString()));
            var other = users.save(AppUserModel.of(UUID.randomUUID().toString()));
            var original = service.create(owner.getId(), "우리집", "관리자", HouseholdType.FAMILY);
            var id = original.household().getId();
            if (withMember) {
                members.save(HouseholdMember.member(original.household(), other, "구성원"));
            }
            var tokenHash = UUID.randomUUID().toString().replace("-", "").repeat(2);
            var invitation = invitations.save(InvitationModel.create(
                    id, tokenHash, 3, Instant.now().plusSeconds(3600)));
            var item = items.save(ItemModel.create(id, "치약", ItemCategory.TOILETRIES, "개", 2, 0));
            return new Fixture(id, owner.getId(), other.getId(), invitation.getId(), item.getId());
        });
    }

    @Test
    void ownerTransfersAndStartsEmptySingleHousehold() {
        var f = fixture(true);
        var result = service.startSingleHousehold(f.owner, f.household, f.other, "내 살림", "나");
        tx().executeWithoutResult(status -> {
            assertThat(result.household().getId()).isNotEqualTo(f.household);
            assertThat(result.household().getHouseholdType()).isEqualTo(HouseholdType.SINGLE);
            assertThat(members.getMember(f.household, f.other).getRole()).isEqualTo(HouseholdMemberRole.OWNER);
            assertThat(members.findById(new HouseholdMemberId(f.household, f.owner))).isEmpty();
            assertThat(members.getMember(result.household().getId(), f.owner).getRole())
                    .isEqualTo(HouseholdMemberRole.OWNER);
            assertThat(invitations.findById(f.invitation)).isEmpty();
            assertThat(households.getHousehold(f.household).getHouseholdType()).isEqualTo(HouseholdType.FAMILY);
            assertThat(items.findById(f.item)).isPresent();
            assertThat(items.findAll()).noneMatch(item -> item.getHouseholdId().equals(result.household().getId()));
        });
    }

    @Test
    void memberLeavesWithoutRevokingOriginalInvitations() {
        var f = fixture(true);
        var result = service.startSingleHousehold(f.other, f.household, null, "내 살림", "나");
        tx().executeWithoutResult(status -> {
            assertThat(members.findById(new HouseholdMemberId(f.household, f.other))).isEmpty();
            assertThat(members.getMember(f.household, f.owner).getRole()).isEqualTo(HouseholdMemberRole.OWNER);
            assertThat(invitations.findById(f.invitation)).isPresent();
            assertThat(result.household().getHouseholdType()).isEqualTo(HouseholdType.SINGLE);
        });
    }

    @Test
    void ownerMustChooseAnotherExistingMember() {
        var f = fixture(true);
        assertThatThrownBy(() -> service.startSingleHousehold(f.owner, f.household, null, "내 살림", "나"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.startSingleHousehold(f.owner, f.household, f.owner, "내 살림", "나"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.startSingleHousehold(f.owner, f.household, UUID.randomUUID(), "내 살림", "나"))
                .isInstanceOf(ApiException.class);
        tx().executeWithoutResult(status -> {
            assertThat(members.getMember(f.household, f.owner).getRole()).isEqualTo(HouseholdMemberRole.OWNER);
            assertThat(invitations.findById(f.invitation)).isPresent();
        });
    }

    @Test
    void rejectsMemberTryingToTransferAndOwnerWithoutOtherMembers() {
        var f = fixture(true);
        assertThatThrownBy(() -> service.startSingleHousehold(f.other, f.household, f.owner, "내 살림", "나"))
                .isInstanceOf(ApiException.class);
        var alone = fixture(false);
        assertThatThrownBy(() -> service.startSingleHousehold(alone.owner, alone.household, null, "내 살림", "나"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void failureRollsBackTransferDepartureInvitationDeletionAndCreation() {
        var f = fixture(true);
        long beforeCount = households.count();
        assertThatThrownBy(() -> tx().executeWithoutResult(status -> {
            service.startSingleHousehold(f.owner, f.household, f.other, "내 살림", "나");
            households.flush();
            throw new IllegalStateException("후속 처리 실패");
        })).isInstanceOf(IllegalStateException.class);
        tx().executeWithoutResult(status -> {
            assertThat(households.count()).isEqualTo(beforeCount);
            assertThat(members.getMember(f.household, f.owner).getRole()).isEqualTo(HouseholdMemberRole.OWNER);
            assertThat(members.getMember(f.household, f.other).getRole()).isEqualTo(HouseholdMemberRole.MEMBER);
            assertThat(invitations.findById(f.invitation)).isPresent();
        });
    }

    private record Fixture(UUID household, UUID owner, UUID other, UUID invitation, UUID item) {}
}
