package com.delivery.payment.application.service;

import com.delivery.payment.application.usecase.CreatePaymentUseCase;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
import com.delivery.payment.domain.payment.exception.InvalidPaymentAmountException;
import com.delivery.payment.port.PaymentMessagingPort;
import com.delivery.payment.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreatePaymentService implements CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentMessagingPort paymentMessagingPort;

    @Override
    @Transactional
    public Payment execute(Payment payment) {
        if (payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentAmountException(payment.getAmount());
        }

        Payment newPayment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(payment.getUserId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Payment saved = paymentRepository.save(newPayment);

        paymentMessagingPort.publishPaymentCreated(saved.getId(), saved.getOrderId());

        return saved;
    }
}
