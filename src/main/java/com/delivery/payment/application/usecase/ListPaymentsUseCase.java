package com.delivery.payment.application.usecase;

import com.delivery.payment.domain.payment.Payment;

import java.util.List;
import java.util.UUID;

public interface ListPaymentsUseCase {
    List<Payment> execute(UUID userId);
}
