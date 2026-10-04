package com.rojojun.familyshare.user;

import com.rojojun.familyshare.household.HouseholdMember;
import com.rojojun.familyshare.household.HouseholdMemberRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final HouseholdMemberRepository householdMemberRepository;

    public AppUserService(AppUserRepository appUserRepository, HouseholdMemberRepository householdMemberRepository) {
        this.appUserRepository = appUserRepository;
        this.householdMemberRepository = householdMemberRepository;
    }

    public Resolution findOrCreate(String anonKey) {
        return appUserRepository.findByAnonKey(anonKey)
                .map(user -> new Resolution(user, false))
                .orElseGet(() -> createOrLoadAfterConflict(anonKey));
    }

    public AppUserDto.MyInformationDto getMyInformation(UUID userId) {
        AppUserDto.User user = appUserRepository.findById(userId)
                .filter(it -> it.getStatus() == AppUserStatus.ACTIVE)
                .map(it -> new AppUserDto.User(
                        it.getId(),
                        it.getStatus(),
                        it.getCreatedAt().atOffset(ZoneOffset.UTC)
                ))
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 유저입니다."));

        List<AppUserDto.HouseholdSummary> householdMember = householdMemberRepository.findHouseholdsForUser(userId)
                .stream()
                .map(row -> new AppUserDto.HouseholdSummary(
                        row.getHouseholdId(),
                        row.getHouseholdName(),
                        row.getMyRole()
                ))
                .toList();

        return new AppUserDto.MyInformationDto(user, householdMember);
    }

    private Resolution createOrLoadAfterConflict(String anonKey) {
        try {
            AppUserModel created = appUserRepository.saveAndFlush(AppUserModel.of(anonKey));
            return new Resolution(created, true);
        } catch (DataIntegrityViolationException e) {
            return appUserRepository.findByAnonKey(anonKey)
                    .map(user -> new Resolution(user, false))
                    .orElseThrow(() -> e);
        }
    }

    public record Resolution(
            AppUserModel appUserModel,
            boolean created
    ) {}
}
