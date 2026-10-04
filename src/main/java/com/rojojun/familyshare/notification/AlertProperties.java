package com.rojojun.familyshare.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** family-share.alert.* : 발송 정책과 토스 스마트 발송 템플릿. */
@ConfigurationProperties(prefix = "family-share.alert")
public record AlertProperties(
        Duration outOfStockDelay,
        Duration reminderInterval,
        Integer maxReminders,
        String templateSetCode
) {
    public AlertProperties {
        outOfStockDelay = outOfStockDelay == null ? Duration.ofHours(3) : outOfStockDelay;
        reminderInterval = reminderInterval == null ? Duration.ofDays(1) : reminderInterval;
        maxReminders = maxReminders == null ? 3 : maxReminders;
        templateSetCode = templateSetCode == null ? "" : templateSetCode;
    }
}
