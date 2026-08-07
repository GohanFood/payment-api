package com.delivery.payment.application.service;

import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
import com.delivery.payment.domain.payment.exception.PaymentAlreadyProcessedException;
import com.delivery.payment.domain.payment.exception.PaymentNotFoundException;
import com.delivery.payment.port.PaymentMessagingPort;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMessagingPort paymentMessagingPort;

    private ProcessPaymentService service;

    @BeforeEach
    void setUp() {
        service = new ProcessPaymentService(paymentRepository, paymentMessagingPort);
    }

    @Test
    void shouldProcessPaymentSuccessfully() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Payment pending = Payment.builder()
                .id(paymentId)
                .userId(UUID.randomUUID())
                .orderId(orderId)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Payment completed = Payment.builder()
                .id(paymentId)
                .userId(pending.getUserId())
                .orderId(orderId)
                .amount(pending.getAmount())
                .paymentMethod(pending.getPaymentMethod())
                .status(PaymentStatus.COMPLETED)
                .gatewayTransactionId("GW-ABC123")
                .createdAt(pending.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any())).thenReturn(completed);

        Payment result = service.execute(paymentId, "token-xyz");

        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getGatewayTransactionId());
        verify(paymentMessagingPort).publishPaymentCompleted(eq(paymentId), eq(orderId));
    }

    @Test
    void shouldThrowExceptionWhenPaymentNotFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () ->
                service.execute(paymentId, "token"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenPaymentAlreadyProcessed() {
        UUID paymentId = UUID.randomUUID();
        Payment completed = Payment.builder()
                .id(paymentId)
                .userId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(completed));

        assertThrows(PaymentAlreadyProcessedException.class, () ->
                service.execute(paymentId, "token"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenPaymentFailed() {
        UUID paymentId = UUID.randomUUID();
        Payment failed = Payment.builder()
                .id(paymentId)
                .userId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.FAILED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(failed));

        assertThrows(PaymentAlreadyProcessedException.class, () ->
                service.execute(paymentId, "token"));
    }

    @Test
    void shouldThrowExceptionWhenPaymentRefunded() {
        UUID paymentId = UUID.randomUUID();
        Payment refunded = Payment.builder()
                .id(paymentId)
                .userId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.REFUNDED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(refunded));

        assertThrows(PaymentAlreadyProcessedException.class, () ->
                service.execute(paymentId, "token"));
    }
}
