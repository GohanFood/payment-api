package com.delivery.payment.adapter.out.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "payment.kafka", name = "enabled", havingValue = "true")
public class KafkaPaymentConsumer {

    @KafkaListener(topics = "order.created", groupId = "payment-group")
    public void handleOrderCreated(String message) {
        log.info("Received order.created event: {}", message);
        // Quando um pedido for criado, podemos criar um pagamento pendente automaticamente
    }

    @KafkaListener(topics = "order.cancelled", groupId = "payment-group")
    public void handleOrderCancelled(String message) {
        log.info("Received order.cancelled event: {}", message);
        // Quando um pedido for cancelado, podemos reembolsar o pagamento
    }
}
