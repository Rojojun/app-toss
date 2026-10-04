package com.rojojun.familyshare.notification;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutOfStockAlertModelTest {

    private static final Instant NOW = Instant.parse("2026-10-04T00:00:00Z");

    private OutOfStockAlertModel open() {
        return OutOfStockAlertModel.open(UUID.randomUUID(), UUID.randomUUID(), "치약", UUID.randomUUID(), NOW, Duration.ofHours(3));
    }

    @Test
    void 처음_알림은_지정한_시간이_지나야_나간다() {
        OutOfStockAlertModel alert = open();

        assertThat(alert.isDue(NOW.plus(Duration.ofHours(2)))).isFalse();
        assertThat(alert.isDue(NOW.plus(Duration.ofHours(3)))).isTrue();
    }

    @Test
    void 정해진_횟수까지만_다시_알리고_끝난다() {
        OutOfStockAlertModel alert = open();
        Instant sentAt = NOW.plus(Duration.ofHours(3));

        assertThat(alert.recordSent(sentAt, Duration.ofDays(1), 3)).isTrue();
        assertThat(alert.isDue(sentAt.plus(Duration.ofHours(23)))).isFalse();
        assertThat(alert.recordSent(sentAt.plus(Duration.ofDays(1)), Duration.ofDays(1), 3)).isTrue();
        assertThat(alert.recordSent(sentAt.plus(Duration.ofDays(2)), Duration.ofDays(1), 3)).isFalse();
    }
}
