package com.delivery.payment.domain.payment.exception;

import java.math.BigDecimal;

public class InvalidPaymentAmountException extends RuntimeException {
    public InvalidPaymentAmountException(BigDecimal amount) {
        super("Valor de pagamento inválido: " + amount);
    }
}
