package com.delivery.payment.application.usecase;

import com.delivery.payment.application.dto.request.CreateCustomerRequest;
import com.delivery.payment.domain.customer.CustomerCardReference;

public interface CreateCustomerUseCase {
    CustomerCardReference execute(CreateCustomerRequest request);
}
