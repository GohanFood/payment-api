package com.delivery.payment.application.usecase;

import com.delivery.payment.domain.payment.Payment;

import java.util.UUID;

public interface RefundPaymentUseCase {
    Payment execute(UUID paymentId);
}
