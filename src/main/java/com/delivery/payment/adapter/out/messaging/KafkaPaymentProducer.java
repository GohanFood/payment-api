package com.delivery.payment.adapter.out.messaging;

import com.delivery.payment.port.PaymentMessagingPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPaymentProducer implements PaymentMessagingPort {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String PAYMENT_CREATED_TOPIC = "payment.created";
    private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";
    private static final String PAYMENT_REFUNDED_TOPIC = "payment.refunded";

    @Override
    public void publishPaymentCreated(UUID paymentId, UUID orderId) {
        String message = buildMessage(paymentId, orderId);
        kafkaTemplate.send(PAYMENT_CREATED_TOPIC, paymentId.toString(), message);
        log.info("Published payment.created: paymentId={}, orderId={}", paymentId, orderId);
    }

    @Override
    public void publishPaymentCompleted(UUID paymentId, UUID orderId) {
        String message = buildMessage(paymentId, orderId);
        kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, paymentId.toString(), message);
        log.info("Published payment.completed: paymentId={}, orderId={}", paymentId, orderId);
    }

    @Override
    public void publishPaymentFailed(UUID paymentId, UUID orderId) {
        String message = buildMessage(paymentId, orderId);
        kafkaTemplate.send(PAYMENT_FAILED_TOPIC, paymentId.toString(), message);
        log.info("Published payment.failed: paymentId={}, orderId={}", paymentId, orderId);
    }

    @Override
    public void publishPaymentRefunded(UUID paymentId, UUID orderId) {
        String message = buildMessage(paymentId, orderId);
        kafkaTemplate.send(PAYMENT_REFUNDED_TOPIC, paymentId.toString(), message);
        log.info("Published payment.refunded: paymentId={}, orderId={}", paymentId, orderId);
    }

    private String buildMessage(UUID paymentId, UUID orderId) {
        return String.format("{\"paymentId\":\"%s\",\"orderId\":\"%s\"}", paymentId, orderId);
    }
}
