package com.delivery.payment.application.service;

import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
import com.delivery.payment.domain.payment.exception.PaymentNotFoundException;
import com.delivery.payment.port.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetPaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    private GetPaymentService service;

    @BeforeEach
    void setUp() {
        service = new GetPaymentService(paymentRepository);
    }

    @Test
    void shouldReturnPaymentWhenFound() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .userId("user-1")
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("150.00"))
                .paymentMethod("PIX")
                .status(PaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        Payment result = service.execute(paymentId);

        assertNotNull(result);
        assertEquals(paymentId, result.getId());
        assertEquals(new BigDecimal("150.00"), result.getAmount());
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
    }

    @Test
    void shouldThrowExceptionWhenPaymentNotFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> service.execute(paymentId));
    }

    @Test
    void shouldReturnPendingPayment() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .userId("user-1")
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("50.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        Payment result = service.execute(paymentId);
        assertEquals(PaymentStatus.PENDING, result.getStatus());
    }
}
