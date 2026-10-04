package com.rojojun.familyshare.notification;

/** 사용자 한 명에게 알림을 보낸다. 실제 발송 수단(토스 스마트 발송)은 구현체가 안다. */
public interface AlertSender {

    void sendOutOfStock(String anonKey, String itemName);
}
