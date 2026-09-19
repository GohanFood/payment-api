package com.delivery.payment.adapter.out.messaging;

import com.delivery.payment.port.PaymentMessagingPort;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Keeps payment processing available when this service is deployed without a
 * Kafka cluster. Subscription status is delivered by the signed HTTP callback.
 */
@Component
@ConditionalOnProperty(prefix = "payment.kafka", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPaymentMessaging implements PaymentMessagingPort {

    @Override
    public void publishPaymentCreated(UUID paymentId, String referenceId) { }

    @Override
    public void publishPaymentCompleted(UUID paymentId, String referenceId) { }

    @Override
    public void publishPaymentFailed(UUID paymentId, String referenceId) { }

    @Override
    public void publishPaymentRefunded(UUID paymentId, String referenceId) { }
}
