package com.delivery.payment.adapter.out.callback;

import com.delivery.payment.config.PaymentCallbackProperties;
import com.delivery.payment.domain.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionPaymentCallbackClient {
    private final PaymentCallbackProperties properties;
    private final RestClient.Builder restClientBuilder;

    public void send(Payment payment) {
        if (!properties.isConfigured() || payment.getReferenceId() == null
                || !payment.getReferenceId().startsWith("subscription:")) {
            return;
        }
        try {
            restClientBuilder.build().post()
                    .uri(properties.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Webhook-Secret", properties.getSecret())
                    .body(new PaymentCallbackPayload(payment))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            // The source payment remains persisted; a later gateway notification can retry delivery.
            log.warn("Falha ao enviar callback de pagamento: paymentId={}, referenceId={}",
                    payment.getId(), payment.getReferenceId(), ex);
        }
    }

    private record PaymentCallbackPayload(
            String referenceId,
            String paymentId,
            String status,
            String paymentMethod,
            BigDecimal amount,
            Long mpPaymentId,
            LocalDateTime occurredAt) {
        PaymentCallbackPayload(Payment payment) {
            this(payment.getReferenceId(), payment.getId().toString(), payment.getStatus().name(),
                    payment.getPaymentMethod(), payment.getAmount(), payment.getMpPaymentId(), LocalDateTime.now());
        }
    }
}
