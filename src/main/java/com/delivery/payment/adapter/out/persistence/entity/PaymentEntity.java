package com.delivery.payment.adapter.out.persistence.entity;

import com.delivery.payment.domain.payment.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payments_user_reference", columnNames = {"user_id", "reference_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "reference_id", nullable = false)
    private String referenceId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId;

    @Column(name = "mp_payment_id")
    private Long mpPaymentId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "card_id")
    private String cardId;

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    @Column(name = "qr_code_base64", columnDefinition = "TEXT")
    private String qrCodeBase64;

    @Column(name = "ticket_url")
    private String ticketUrl;

    @Column(name = "payer_email")
    private String payerEmail;

    @Column(name = "payer_document_type")
    private String payerDocumentType;

    @Column(name = "payer_document_number")
    private String payerDocumentNumber;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
