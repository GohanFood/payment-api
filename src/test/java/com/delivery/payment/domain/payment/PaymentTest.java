package com.delivery.payment.domain.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    @Test
    void shouldCreatePaymentWithAllFields() {
        UUID id = UUID.randomUUID();
        String userId = "user-1";
        String referenceId = "subscription:" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Payment payment = Payment.builder()
                .id(id)
                .userId(userId)
                .referenceId(referenceId)
                .amount(new BigDecimal("150.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertNotNull(payment);
        assertEquals(id, payment.getId());
        assertEquals(userId, payment.getUserId());
        assertEquals(referenceId, payment.getReferenceId());
        assertEquals(new BigDecimal("150.00"), payment.getAmount());
        assertEquals("CREDIT_CARD", payment.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertNull(payment.getGatewayTransactionId());
    }

    @Test
    void shouldMarkAsProcessing() {
        Payment payment = createPendingPayment();
        payment.markAsProcessing();
        assertEquals(PaymentStatus.PROCESSING, payment.getStatus());
    }

    @Test
    void shouldMarkAsCompleted() {
        Payment payment = createPendingPayment();
        payment.markAsCompleted("GW-ABC123");
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals("GW-ABC123", payment.getGatewayTransactionId());
    }

    @Test
    void shouldMarkAsFailed() {
        Payment payment = createPendingPayment();
        payment.markAsFailed();
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }

    @Test
    void shouldMarkAsRefunded() {
        Payment payment = createPendingPayment();
        payment.markAsRefunded();
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
    }

    @Test
    void shouldHandleAllStatusTransitions() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .userId("user-1")
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("50.00"))
                .paymentMethod("PIX")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // PENDING → PROCESSING → COMPLETED
        payment.markAsProcessing();
        assertEquals(PaymentStatus.PROCESSING, payment.getStatus());

        payment.markAsCompleted("TX-123");
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals("TX-123", payment.getGatewayTransactionId());
    }

    @Test
    void shouldHandleFailureThenRefundTransition() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .userId("user-1")
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("200.00"))
                .paymentMethod("DEBIT_CARD")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        payment.markAsFailed();
        assertEquals(PaymentStatus.FAILED, payment.getStatus());

        payment.markAsRefunded();
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
    }

    @Test
    void shouldAllowZeroAmountInDomain() {
        // Domain não valida — validação fica no Service/DTO
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .userId("user-1")
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(BigDecimal.ZERO)
                .paymentMethod("PIX")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getAmount());
    }

    @Test
    void shouldBuildPaymentWithCorrectAmountPrecision() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .userId("user-1")
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("99.99"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        assertEquals(0, new BigDecimal("99.99").compareTo(payment.getAmount()));
    }

    private Payment createPendingPayment() {
        return Payment.builder()
                .id(UUID.randomUUID())
                .userId("user-1")
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
