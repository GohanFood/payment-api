package com.delivery.payment.adapter.out.messaging;

import com.delivery.payment.port.PaymentMessagingPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "payment.kafka", name = "enabled", havingValue = "true")
public class KafkaPaymentProducer implements PaymentMessagingPort {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String PAYMENT_CREATED_TOPIC = "payment.created";
    private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";
    private static final String PAYMENT_REFUNDED_TOPIC = "payment.refunded";

    @Override
    public void publishPaymentCreated(UUID paymentId, String referenceId) {
        String message = buildMessage(paymentId, referenceId);
        kafkaTemplate.send(PAYMENT_CREATED_TOPIC, paymentId.toString(), message);
        log.info("Published payment.created: paymentId={}, referenceId={}", paymentId, referenceId);
    }

    @Override
    public void publishPaymentCompleted(UUID paymentId, String referenceId) {
        String message = buildMessage(paymentId, referenceId);
        kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, paymentId.toString(), message);
        log.info("Published payment.completed: paymentId={}, referenceId={}", paymentId, referenceId);
    }

    @Override
    public void publishPaymentFailed(UUID paymentId, String referenceId) {
        String message = buildMessage(paymentId, referenceId);
        kafkaTemplate.send(PAYMENT_FAILED_TOPIC, paymentId.toString(), message);
        log.info("Published payment.failed: paymentId={}, referenceId={}", paymentId, referenceId);
    }

    @Override
    public void publishPaymentRefunded(UUID paymentId, String referenceId) {
        String message = buildMessage(paymentId, referenceId);
        kafkaTemplate.send(PAYMENT_REFUNDED_TOPIC, paymentId.toString(), message);
        log.info("Published payment.refunded: paymentId={}, referenceId={}", paymentId, referenceId);
    }

    private String buildMessage(UUID paymentId, String referenceId) {
        return String.format("{\"paymentId\":\"%s\",\"referenceId\":\"%s\"}", paymentId, referenceId);
    }
}
