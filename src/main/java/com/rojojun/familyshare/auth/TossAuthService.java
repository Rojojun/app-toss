package com.rojojun.familyshare.auth;

import com.rojojun.familyshare.notification.SlackWebhookSender;
import com.rojojun.familyshare.user.AppUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class TossAuthService {

    private final TossIdentityProvider tossIdentityProvider;
    private final AppUserService appUserService;
    private final ApiTokenIssuer apiTokenIssuer;
    private final SlackWebhookSender slackWebhookSender;

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

        if (resolution.created()) {
            slackWebhookSender.sendToText()
                    .apply("새로운 유저가 가입했어요")
                    .peekLeft(error -> log.warn("Slack 가입 알람 실패: {}", error.message(), error. cause()));
        }

        return new ExchangeResult(resolution.created(), response);
    }

    public record ExchangeResult(
            boolean newUserCreated,
            TossAuthDto.AuthResponse response
    ) {
    }
}
