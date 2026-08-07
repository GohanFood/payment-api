package com.delivery.payment.port;

import java.util.UUID;

public interface PaymentMessagingPort {

    void publishPaymentCreated(UUID paymentId, UUID orderId);

    void publishPaymentCompleted(UUID paymentId, UUID orderId);

    void publishPaymentFailed(UUID paymentId, UUID orderId);
}
