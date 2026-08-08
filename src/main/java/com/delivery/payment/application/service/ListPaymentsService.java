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

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final PaymentRepository paymentRepository;

    @Override
    public List<Payment> execute(UUID userId) {
        return execute(userId, 0, DEFAULT_PAGE_SIZE);
    }

    @Override
    public List<Payment> execute(UUID userId, int page, int size) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        return paymentRepository.findByUserId(userId, page, safeSize);
    }

    @Override
    public long countByUserId(UUID userId) {
        return paymentRepository.countByUserId(userId);
    }
}
