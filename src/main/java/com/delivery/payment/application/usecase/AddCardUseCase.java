package com.delivery.payment.application.usecase;

import com.delivery.payment.application.dto.request.AddCardRequest;
import com.delivery.payment.domain.customer.CustomerCardReference;

public interface AddCardUseCase {
    CustomerCardReference execute(String customerId, AddCardRequest request);
}
