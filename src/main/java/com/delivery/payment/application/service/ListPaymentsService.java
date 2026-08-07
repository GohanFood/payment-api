package com.delivery.payment.application.service;

import com.delivery.payment.application.usecase.ListPaymentsUseCase;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListPaymentsService implements ListPaymentsUseCase {

    private final PaymentRepository paymentRepository;

    @Override
    public List<Payment> execute(UUID userId) {
        return paymentRepository.findByUserId(userId);
    }
}
