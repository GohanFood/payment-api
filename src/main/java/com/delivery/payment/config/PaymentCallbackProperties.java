package com.delivery.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "payment-callback")
public class PaymentCallbackProperties {
    private String url;
    private String secret;

    public boolean isConfigured() {
        return url != null && !url.isBlank() && secret != null && !secret.isBlank();
    }
}
