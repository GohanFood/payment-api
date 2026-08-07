package com.delivery.payment.domain.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class Payment {

    private UUID id;
    private UUID userId;
    private UUID orderId;
    private BigDecimal amount;
    private String paymentMethod;
    private PaymentStatus status;
    private String gatewayTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void markAsProcessing() {
        this.status = PaymentStatus.PROCESSING;
    }

    public void markAsCompleted(String gatewayTransactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public void markAsFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void markAsRefunded() {
        this.status = PaymentStatus.REFUNDED;
    }
}
