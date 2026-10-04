package com.rojojun.familyshare.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class TossApiClientConfiguration {

    @Bean
    public RestClient tossRestClient(
            SslBundles sslBundles,
            @Value("${family-share.toss.base-url}") String baseUrl,
            @Value("${family-share.toss.ssl-bundle:}") String sslBundleName
    ) throws Exception {
        SSLContext sslContext = sslBundleName.isBlank()
                ? SSLContext.getDefault()
                : sslBundles.getBundle(sslBundleName).createSslContext();

        HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
