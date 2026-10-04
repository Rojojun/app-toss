package com.rojojun.familyshare.auth;

import com.rojojun.familyshare.user.AppUserService;
import org.springframework.stereotype.Service;

@Service
public class TossAuthService {

    private final TossIdentityProvider tossIdentityProvider;
    private final AppUserService appUserService;
    private final ApiTokenIssuer apiTokenIssuer;

    public TossAuthService(TossIdentityProvider tossIdentityProvider, AppUserService appUserService, ApiTokenIssuer apiTokenIssuer) {
        this.tossIdentityProvider = tossIdentityProvider;
        this.appUserService = appUserService;
        this.apiTokenIssuer = apiTokenIssuer;
    }

    public ExchangeResult exchange(TossAuthDto.ExchangeRequest request) {
        TossIdentityProvider.VerifiedIdentity identity = tossIdentityProvider.verify(request.code());

        AppUserService.Resolution resolution = appUserService.findOrCreate(identity.anonKey());
        var user = resolution.appUserModel();

        user.requireActive();

        ApiTokenIssuer.IssuedToken issuedToken = apiTokenIssuer.issue(user.getId());
        TossAuthDto.AuthResponse response = new TossAuthDto.AuthResponse(
                issuedToken.accessToken(),
                "Bearer",
                issuedToken.expiresInSeconds(),
                TossAuthDto.UserResponse.from(user)
        );

        return new ExchangeResult(resolution.created(), response);
    }

    public record ExchangeResult(
            boolean newUserCreated,
            TossAuthDto.AuthResponse response
    ) {
    }
}
