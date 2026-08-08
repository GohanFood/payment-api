package com.delivery.payment.application.service;

import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
import com.delivery.payment.port.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListPaymentsServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    private ListPaymentsService service;

    @BeforeEach
    void setUp() {
        service = new ListPaymentsService(paymentRepository);
    }

    @Test
    void shouldReturnPaymentsForUser() {
        UUID userId = UUID.randomUUID();
        Payment p1 = Payment.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .orderId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Payment p2 = Payment.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .orderId(UUID.randomUUID())
                .amount(new BigDecimal("50.00"))
                .paymentMethod("PIX")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findByUserId(eq(userId), eq(0), eq(20))).thenReturn(List.of(p1, p2));

        List<Payment> result = service.execute(userId);

        assertEquals(2, result.size());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals(userId, result.get(1).getUserId());
    }

    @Test
    void shouldReturnEmptyListWhenNoPayments() {
        UUID userId = UUID.randomUUID();
        when(paymentRepository.findByUserId(eq(userId), eq(0), eq(20))).thenReturn(Collections.emptyList());

        List<Payment> result = service.execute(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnSinglePayment() {
        UUID userId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .orderId(UUID.randomUUID())
                .amount(new BigDecimal("200.00"))
                .paymentMethod("DEBIT_CARD")
                .status(PaymentStatus.FAILED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findByUserId(eq(userId), eq(0), eq(20))).thenReturn(List.of(payment));

        List<Payment> result = service.execute(userId);

        assertEquals(1, result.size());
        assertEquals(PaymentStatus.FAILED, result.get(0).getStatus());
    }
}
