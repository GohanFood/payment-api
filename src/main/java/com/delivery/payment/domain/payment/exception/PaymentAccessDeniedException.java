package com.delivery.payment.domain.payment.exception;

import java.util.UUID;

/** Raised when an authenticated user attempts to access another user's payment. */
public class PaymentAccessDeniedException extends RuntimeException {

    public PaymentAccessDeniedException(UUID paymentId) {
        super("Acesso negado ao pagamento: " + paymentId);
    }
}
