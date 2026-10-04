package com.rojojun.familyshare.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 토스 스마트 발송(기능성 메시지). 사용자 식별키는 anonKey 이므로 x-toss-user-key 가 아니라 x-anon-key 로 보낸다.
 * 템플릿(templateSetCode)은 콘솔에서 만들고 카피 리뷰 승인을 받은 것이어야 한다.
 */
@RequiredArgsConstructor
@Component
@ConditionalOnExpression("!'${family-share.alert.template-set-code:}'.isEmpty()")
public class TossSmartMessageSender implements AlertSender {

    // 알림 본문이 한 줄에 들어가도록, 템플릿 변수(물품 이름)는 이 길이를 넘으면 줄인다.
    static final int MAX_ITEM_NAME_LENGTH = 8;

    private static final String SEND_PATH = "/api-partner/v1/apps-in-toss/messenger/send-message";

    private final RestClient tossRestClient;
    private final AlertProperties alertProperties;

    @Override
    public void sendOutOfStock(String anonKey, String itemName) {
        tossRestClient.post()
                .uri(SEND_PATH)
                .header("x-anon-key", anonKey)
                .body(Map.of(
                        "templateSetCode", alertProperties.templateSetCode(),
                        "context", Map.of("itemName", shorten(itemName))))
                .retrieve()
                .toBodilessEntity();
    }

    static String shorten(String itemName) {
        return itemName.length() <= MAX_ITEM_NAME_LENGTH
                ? itemName
                : itemName.substring(0, MAX_ITEM_NAME_LENGTH - 1).stripTrailing() + "…";
    }
}
