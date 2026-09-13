package com.delivery.payment.port;

import java.util.UUID;

public interface PaymentMessagingPort {

    void publishPaymentCreated(UUID paymentId, String referenceId);

    void publishPaymentCompleted(UUID paymentId, String referenceId);

    void publishPaymentFailed(UUID paymentId, String referenceId);

    void publishPaymentRefunded(UUID paymentId, String referenceId);
}
