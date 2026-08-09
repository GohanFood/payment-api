package com.delivery.payment.application.usecase;

import com.delivery.payment.application.dto.request.CreatePaymentRequest;
import com.delivery.payment.domain.payment.Payment;

public interface CreatePaymentUseCase {
    Payment execute(CreatePaymentRequest request, String userId);
}
