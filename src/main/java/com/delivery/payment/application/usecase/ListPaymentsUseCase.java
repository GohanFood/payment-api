package com.delivery.payment.application.usecase;

import com.delivery.payment.domain.payment.Payment;

import java.util.List;

public interface ListPaymentsUseCase {
    List<Payment> execute(String userId);

    List<Payment> execute(String userId, int page, int size);

    long countByUserId(String userId);
}
