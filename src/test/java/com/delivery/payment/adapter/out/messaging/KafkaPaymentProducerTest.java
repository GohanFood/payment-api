package com.delivery.payment.adapter.out.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaPaymentProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaPaymentProducer producer;

    @BeforeEach
    void setUp() {
        producer = new KafkaPaymentProducer(kafkaTemplate);
    }

    @Test
    void shouldPublishPaymentCreated() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        producer.publishPaymentCreated(paymentId, orderId);

        verify(kafkaTemplate).send(eq("payment.created"), eq(paymentId.toString()), anyString());
    }

    @Test
    void shouldPublishPaymentCompleted() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        producer.publishPaymentCompleted(paymentId, orderId);

        verify(kafkaTemplate).send(eq("payment.completed"), eq(paymentId.toString()), anyString());
    }

    @Test
    void shouldPublishPaymentFailed() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        producer.publishPaymentFailed(paymentId, orderId);

        verify(kafkaTemplate).send(eq("payment.failed"), eq(paymentId.toString()), anyString());
    }

    @Test
    void shouldIncludePaymentAndOrderIdInMessage() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        producer.publishPaymentCreated(paymentId, orderId);

        verify(kafkaTemplate).send(
                eq("payment.created"),
                eq(paymentId.toString()),
                contains(paymentId.toString()));
    }

    private String contains(String substring) {
        return argThat(s -> s != null && s.contains(substring));
    }
}
