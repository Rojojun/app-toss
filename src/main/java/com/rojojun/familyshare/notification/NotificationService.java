package com.rojojun.familyshare.notification;

import com.rojojun.familyshare.household.HouseholdMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class NotificationService {

    private final AlertPreferenceRepository alertPreferenceRepository;
    private final HouseholdMemberRepository memberRepository;

    @Transactional(readOnly = true)
    public AlertPreferenceModel getAlertPreference(UUID userId, UUID householdId) {
        memberRepository.getMember(householdId, userId);

        return alertPreferenceRepository.findById(new AlertPreferenceId(householdId, userId))
                .orElseGet(() -> AlertPreferenceModel.of(householdId, userId, false));
    }

    @Transactional
    public AlertPreferenceModel putAlertPreference(UUID userId, UUID householdId, boolean enabled) {
        memberRepository.getMember(householdId, userId);

        return alertPreferenceRepository.save(AlertPreferenceModel.of(householdId, userId, enabled));
    }
}
