package com.rojojun.familyshare.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/** 템플릿 코드가 없으면(카피 리뷰 승인 전) 실제로 보내지 않고 기록만 한다. */
@Slf4j
@Component
@ConditionalOnExpression("'${family-share.alert.template-set-code:}'.isEmpty()")
public class LoggingAlertSender implements AlertSender {

    @Override
    public void sendOutOfStock(String anonKey, String itemName) {
        log.info("[알림 미발송: 템플릿 코드 없음] '{}' 다 떨어졌어요", itemName);
    }
}
