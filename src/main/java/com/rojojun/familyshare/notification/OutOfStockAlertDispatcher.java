package com.rojojun.familyshare.notification;

import com.rojojun.familyshare.household.HouseholdMember;
import com.rojojun.familyshare.household.HouseholdMemberRepository;
import com.rojojun.familyshare.user.AppUserModel;
import com.rojojun.familyshare.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 0이 된 채 시간이 지난 물품을, 그 물품을 0으로 만든 사람을 뺀 구성원에게 알린다. */
@Slf4j
@RequiredArgsConstructor
@Service
public class OutOfStockAlertDispatcher {

    private final OutOfStockAlertRepository outOfStockAlertRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final AppUserRepository appUserRepository;
    private final AlertSender alertSender;
    private final AlertProperties alertProperties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${family-share.alert.poll-interval:60000}")
    @Transactional
    public void dispatchDue() {
        Instant now = Instant.now(clock);

        outOfStockAlertRepository.findDue(now).forEach(alert -> {
            List<UUID> recipientIds = householdMemberRepository.findAllByIdHouseholdIdOrderByJoinedAt(alert.getHouseholdId()).stream()
                    .map(HouseholdMember::getUserId)
                    .filter(userId -> !userId.equals(alert.getActorUserId()))
                    .toList();

            appUserRepository.findAllById(recipientIds).stream()
                    .filter(AppUserModel::isActive)
                    .map(AppUserModel::getAnonKey)
                    .forEach(anonKey -> {
                        try {
                            alertSender.sendOutOfStock(anonKey, alert.getItemName());
                        } catch (RuntimeException e) {
                            log.warn("알림 발송 실패 item={} : {}", alert.getItemId(), e.getMessage());
                        }
                    });

            boolean more = alert.recordSent(now, alertProperties.reminderInterval(), alertProperties.maxReminders());
            if (!more) {
                outOfStockAlertRepository.delete(alert);
            }
        });
    }
}
