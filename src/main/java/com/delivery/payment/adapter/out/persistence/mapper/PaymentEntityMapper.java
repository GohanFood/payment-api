package com.delivery.payment.adapter.out.persistence.mapper;

import com.delivery.payment.adapter.out.persistence.entity.PaymentEntity;
import com.delivery.payment.domain.payment.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentEntityMapper {

    public PaymentEntity toEntity(Payment payment) {
        return PaymentEntity.builder()
                .id(payment.getId())
                .userId(payment.getUserId())
                .referenceId(payment.getReferenceId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .mpPaymentId(payment.getMpPaymentId())
                .qrCode(payment.getQrCode())
                .qrCodeBase64(payment.getQrCodeBase64())
                .ticketUrl(payment.getTicketUrl())
                .payerEmail(payment.getPayerEmail())
                .payerDocumentType(payment.getPayerDocumentType())
                .payerDocumentNumber(payment.getPayerDocumentNumber())
                .expiresAt(payment.getExpiresAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    public Payment toDomain(PaymentEntity entity) {
        return Payment.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .referenceId(entity.getReferenceId())
                .amount(entity.getAmount())
                .paymentMethod(entity.getPaymentMethod())
                .status(entity.getStatus())
                .gatewayTransactionId(entity.getGatewayTransactionId())
                .mpPaymentId(entity.getMpPaymentId())
                .qrCode(entity.getQrCode())
                .qrCodeBase64(entity.getQrCodeBase64())
                .ticketUrl(entity.getTicketUrl())
                .payerEmail(entity.getPayerEmail())
                .payerDocumentType(entity.getPayerDocumentType())
                .payerDocumentNumber(entity.getPayerDocumentNumber())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
