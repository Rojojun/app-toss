package com.rojojun.familyshare.notification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TossSmartMessageSenderTest {

    @Test
    void 물품_이름이_길면_여덟_글자로_줄여_보낸다() {
        assertThat(TossSmartMessageSender.shorten("치약")).isEqualTo("치약");
        assertThat(TossSmartMessageSender.shorten("욕실 세정제 대용량")).isEqualTo("욕실 세정제…");
    }
}
