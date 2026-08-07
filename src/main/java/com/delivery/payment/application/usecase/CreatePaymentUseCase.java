package com.delivery.payment.application.usecase;

import com.delivery.payment.domain.payment.Payment;

public interface CreatePaymentUseCase {
    Payment execute(Payment payment);
}
