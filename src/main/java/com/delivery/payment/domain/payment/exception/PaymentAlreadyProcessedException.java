package com.delivery.payment.domain.payment.exception;

import java.util.UUID;

public class PaymentAlreadyProcessedException extends RuntimeException {
    public PaymentAlreadyProcessedException(UUID id) {
        super("Pagamento já foi processado: " + id);
    }
}
