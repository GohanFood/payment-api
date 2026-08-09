package com.delivery.payment.adapter.out.persistence.mapper;

import com.delivery.payment.adapter.out.persistence.entity.PaymentEntity;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentEntityMapperTest {

    private PaymentEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PaymentEntityMapper();
    }

    @Test
    void shouldMapDomainToEntity() {
        UUID id = UUID.randomUUID();
        String userId = "user-1";
        UUID orderId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Payment domain = Payment.builder()
                .id(id)
                .userId("user-1")
                .orderId(orderId)
                .amount(new BigDecimal("150.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.PENDING)
                .gatewayTransactionId(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        PaymentEntity entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals("user-1", entity.getUserId());
        assertEquals(orderId, entity.getOrderId());
        assertEquals(new BigDecimal("150.00"), entity.getAmount());
        assertEquals("CREDIT_CARD", entity.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, entity.getStatus());
        assertNull(entity.getGatewayTransactionId());
        assertEquals(now, entity.getCreatedAt());
    }

    @Test
    void shouldMapEntityToDomain() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        PaymentEntity entity = PaymentEntity.builder()
                .id(id)
                .userId("user-1")
                .orderId(UUID.randomUUID())
                .amount(new BigDecimal("200.00"))
                .paymentMethod("PIX")
                .status(PaymentStatus.COMPLETED)
                .gatewayTransactionId("GW-123")
                .createdAt(now)
                .updatedAt(now)
                .build();

        Payment domain = mapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(id, domain.getId());
        assertEquals(new BigDecimal("200.00"), domain.getAmount());
        assertEquals("PIX", domain.getPaymentMethod());
        assertEquals(PaymentStatus.COMPLETED, domain.getStatus());
        assertEquals("GW-123", domain.getGatewayTransactionId());
    }

    @Test
    void shouldHandleNullGatewayTransactionId() {
        PaymentEntity entity = PaymentEntity.builder()
                .id(UUID.randomUUID())
                .userId("user-1")
                .orderId(UUID.randomUUID())
                .amount(new BigDecimal("50.00"))
                .paymentMethod("DEBIT_CARD")
                .status(PaymentStatus.FAILED)
                .gatewayTransactionId(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Payment domain = mapper.toDomain(entity);
        assertNull(domain.getGatewayTransactionId());
    }

    @Test
    void shouldRoundTripCorrectly() {
        UUID id = UUID.randomUUID();
        String userId = "user-1";
        UUID orderId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Payment original = Payment.builder()
                .id(id)
                .userId("user-1")
                .orderId(orderId)
                .amount(new BigDecimal("99.99"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.REFUNDED)
                .gatewayTransactionId("GW-RFND-001")
                .createdAt(now)
                .updatedAt(now)
                .build();

        PaymentEntity entity = mapper.toEntity(original);
        Payment roundTripped = mapper.toDomain(entity);

        assertEquals(original.getId(), roundTripped.getId());
        assertEquals(original.getUserId(), roundTripped.getUserId());
        assertEquals(original.getOrderId(), roundTripped.getOrderId());
        assertEquals(original.getAmount(), roundTripped.getAmount());
        assertEquals(original.getPaymentMethod(), roundTripped.getPaymentMethod());
        assertEquals(original.getStatus(), roundTripped.getStatus());
        assertEquals(original.getGatewayTransactionId(), roundTripped.getGatewayTransactionId());
    }

    @Test
    void shouldHandleVariousPaymentStatuses() {
        for (PaymentStatus status : PaymentStatus.values()) {
            PaymentEntity entity = PaymentEntity.builder()
                    .id(UUID.randomUUID())
                    .userId("user-1")
                    .orderId(UUID.randomUUID())
                    .amount(new BigDecimal("100.00"))
                    .paymentMethod("CREDIT_CARD")
                    .status(status)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Payment domain = mapper.toDomain(entity);
            assertEquals(status, domain.getStatus());
        }
    }
}
