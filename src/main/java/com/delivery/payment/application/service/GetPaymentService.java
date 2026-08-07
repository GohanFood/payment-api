package com.delivery.payment.application.service;

import com.delivery.payment.application.usecase.GetPaymentUseCase;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.exception.PaymentNotFoundException;
import com.delivery.payment.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPaymentService implements GetPaymentUseCase {

    private final PaymentRepository paymentRepository;

    @Override
    public Payment execute(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
