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
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    public Payment toDomain(PaymentEntity entity) {
        return Payment.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .orderId(entity.getOrderId())
                .amount(entity.getAmount())
                .paymentMethod(entity.getPaymentMethod())
                .status(entity.getStatus())
                .gatewayTransactionId(entity.getGatewayTransactionId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
