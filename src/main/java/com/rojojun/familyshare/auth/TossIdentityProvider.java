package com.rojojun.familyshare.auth;

public interface TossIdentityProvider {
    VerifiedIdentity verify(String code);

    record VerifiedIdentity(String anonKey) {
        public VerifiedIdentity {
            if (anonKey == null || anonKey.isBlank()) {
                throw new IllegalArgumentException("토스 익명 사용자 식별키가 없습니다.");
            }
        }
    }
}
