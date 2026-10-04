package com.rojojun.familyshare.auth;

import java.util.UUID;

public interface ApiTokenIssuer {

    IssuedToken issue(UUID appUserID);

    record IssuedToken(
            String accessToken,
            long expiresInSeconds
    ) {}
}
