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
    private String userId;
    private String referenceId;
    private BigDecimal amount;
    private String paymentMethod;
    private PaymentStatus status;
    private String gatewayTransactionId;
    private Long mpPaymentId;
    private String customerId;
    private String cardId;
    private String qrCode;
    private String qrCodeBase64;
    private String ticketUrl;
    private String payerEmail;
    private String payerDocumentType;
    private String payerDocumentNumber;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void markAsProcessing() {
        this.status = PaymentStatus.PROCESSING;
    }

    public void markAsCompleted(String gatewayTransactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.gatewayTransactionId = gatewayTransactionId;
        try {
            this.mpPaymentId = Long.valueOf(gatewayTransactionId);
        } catch (NumberFormatException ignored) {
            // Test gateways may return non-numeric transaction ids.
        }
    }

    public void markAsFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void markAsRefunded() {
        this.status = PaymentStatus.REFUNDED;
    }

    public void linkMercadoPago(Long mpPaymentId, String qrCode, String qrCodeBase64, String ticketUrl, LocalDateTime expiresAt) {
        this.mpPaymentId = mpPaymentId;
        this.qrCode = qrCode;
        this.qrCodeBase64 = qrCodeBase64;
        this.ticketUrl = ticketUrl;
        this.expiresAt = expiresAt;
    }

    public boolean isPix() {
        return "PIX".equalsIgnoreCase(paymentMethod);
    }

    public boolean isPending() {
        return PaymentStatus.PENDING.equals(status);
    }
}
