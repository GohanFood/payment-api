package com.delivery.payment.application.dto.response;

import com.delivery.payment.domain.payment.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;
    private UUID userId;
    private UUID orderId;
    private BigDecimal amount;
    private String paymentMethod;
    private PaymentStatus status;
    private String gatewayTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
