package com.rojojun.familyshare.notification;

import com.slack.api.Slack;
import com.slack.api.webhook.Payload;
import com.slack.api.webhook.WebhookResponse;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class SlackWebhookSender {

    private String webhookUrl;
    private final Slack slack = Slack.getInstance();

    public SlackWebhookSender(@Value("${slack.webhook.url:}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public Function<String, Either<SlackSendError, WebhookResponse>> sendToText() {
        return this::sendText;
    }

    private Either<SlackSendError, WebhookResponse> sendText(String text) {
        if (webhookUrl.isBlank()) {
            return Either.left(new SlackSendError("Slack webhook URL이 설정되지 않았습니다.", null));
        }

        var payload = Payload.builder().text(text).build();

        return Try.of(() -> slack.send(webhookUrl, payload))
                .toEither()
                .mapLeft(error -> new SlackSendError("Slack 요청 중 오류가 발생했습니다.", error))
                .flatMap(this::checkResponse);
    }

    private Either<SlackSendError, WebhookResponse> checkResponse(WebhookResponse response) {
        if (Integer.valueOf(200).equals(response.getCode())) {
            return Either.right(response);
        }

        return Either.left(new SlackSendError(
                "Slack 응답 오류: Http " + response.getCode() + ", " + response.getBody(),
                null
        ));
    }

    public record SlackSendError(String message, Throwable cause) {}
}
