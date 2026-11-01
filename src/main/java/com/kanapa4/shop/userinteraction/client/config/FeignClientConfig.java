package com.kanapa4.shop.userinteraction.client.config;

import feign.Client;
import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import feign.hc5.ApacheHttp5Client;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    @Bean
    public Client feignClient(CloseableHttpClient httpClient) {
        return new ApacheHttp5Client(httpClient);
    }

    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(200, 1000, 3);
    }

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        ErrorDecoder defaultDecoder = new ErrorDecoder.Default();
        return (methodKey, response) -> {
            if (response.status() == 503) {
                return new RetryableException(
                        response.status(),
                        "Downstream service returned 503 - retrying",
                        response.request().httpMethod(),
                        (Long) null,
                        response.request());
            }
            return defaultDecoder.decode(methodKey, response);
        };
    }

    @Bean
    public CloseableHttpClient feignHttpClient() {
        return HttpClients.custom()
                .disableAutomaticRetries()
                .disableCookieManagement()
                .useSystemProperties()
                .build();
    }
}
