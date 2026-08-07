package com.delivery.payment.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private UUID orderId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private String paymentMethod;

    // --- Dados do pagador (obrigatórios para PIX via Mercado Pago) ---

    private String payerEmail;
    private String payerFirstName;
    private String payerLastName;
    private String payerDocumentType;
    private String payerDocumentNumber;
}
