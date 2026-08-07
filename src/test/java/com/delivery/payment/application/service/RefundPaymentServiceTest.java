package com.delivery.payment.application.service;

import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
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
class RefundPaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMessagingPort paymentMessagingPort;

    private RefundPaymentService service;

    @BeforeEach
    void setUp() {
        service = new RefundPaymentService(paymentRepository, paymentMessagingPort);
    }

    @Test
    void shouldRefundPaymentSuccessfully() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Payment original = Payment.builder()
                .id(paymentId)
                .userId(UUID.randomUUID())
                .orderId(orderId)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.COMPLETED)
                .gatewayTransactionId("GW-ABC")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Payment refunded = Payment.builder()
                .id(paymentId)
                .userId(original.getUserId())
                .orderId(orderId)
                .amount(original.getAmount())
                .paymentMethod(original.getPaymentMethod())
                .status(PaymentStatus.REFUNDED)
                .gatewayTransactionId(original.getGatewayTransactionId())
                .createdAt(original.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(original));
        when(paymentRepository.save(any())).thenReturn(refunded);

        Payment result = service.execute(paymentId);

        assertNotNull(result);
        assertEquals(PaymentStatus.REFUNDED, result.getStatus());
        verify(paymentMessagingPort).publishPaymentFailed(eq(paymentId), eq(orderId));
    }

    @Test
    void shouldThrowExceptionWhenPaymentNotFoundForRefund() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> service.execute(paymentId));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldRefundPendingPayment() {
        UUID paymentId = UUID.randomUUID();
        Payment pending = Payment.builder()
                .id(paymentId)
                .userId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .amount(new BigDecimal("75.00"))
                .paymentMethod("PIX")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any())).thenReturn(pending);

        Payment result = service.execute(paymentId);
        assertEquals(PaymentStatus.REFUNDED, result.getStatus());
    }

    @Test
    void shouldRefundCompletedPayment() {
        UUID paymentId = UUID.randomUUID();
        Payment completed = Payment.builder()
                .id(paymentId)
                .userId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .amount(new BigDecimal("300.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.COMPLETED)
                .gatewayTransactionId("GW-XYZ")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(completed));
        when(paymentRepository.save(any())).thenReturn(completed);

        Payment result = service.execute(paymentId);
        assertEquals(PaymentStatus.REFUNDED, result.getStatus());
        verify(paymentMessagingPort).publishPaymentFailed(any(), any());
    }
}
