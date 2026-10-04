package com.rojojun.familyshare.household;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public class HouseholdDto {

    public record NameRequest(@NotBlank @Size(max = 100) String name) {}

    public record CreateRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 20) String nickname
    ) {}

    public record NicknameRequest(@NotBlank @Size(max = 20) String nickname) {}

    public record TransferRequest(@NotNull UUID newOwnerUserId) {}

    public record Detail(
            UUID id,
            String name,
            HouseholdMemberRole myRole,
            String myNickname,
            List<MemberResponse> members,
            OffsetDateTime createdAt
    ) {
        static Detail from(HouseholdService.Snapshot snapshot) {
            return new Detail(
                    snapshot.household().getId(),
                    snapshot.household().getName(),
                    snapshot.me().getRole(),
                    snapshot.me().getNickname(),
                    snapshot.members().stream().map(MemberResponse::from).toList(),
                    snapshot.household().getCreatedAt().atOffset(ZoneOffset.UTC)
            );
        }
    }

    public record MemberResponse(UUID householdId, UUID userId, String nickname, HouseholdMemberRole role, OffsetDateTime joinedAt) {
        static MemberResponse from(HouseholdMember member) {
            return new MemberResponse(
                    member.getHouseholdId(),
                    member.getUserId(),
                    member.getNickname(),
                    member.getRole(),
                    member.getJoinedAt().atOffset(ZoneOffset.UTC)
            );
        }
    }
}
