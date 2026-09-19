package com.delivery.payment.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull
    private String referenceId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private String paymentMethod;

    // --- Dados do pagador (obrigatórios para PIX via Mercado Pago) ---

    private String payerEmail;
    private String payerDocumentType;
    private String payerDocumentNumber;

    // --- Dados do cartão (obrigatórios para CREDIT_CARD/DEBIT_CARD via Mercado Pago) ---

    /** CardToken gerado pelo MercadoPago.js CardForm */
    private String gatewayToken;

    /** Cartão salvo (recorrência). Alternativa a {@code gatewayToken}. */
    private String cardId;

    /** ID do Customer no Mercado Pago (obrigatório quando usa {@code cardId}). */
    private String customerId;

    /** Número de parcelas (default: 1) */
    @Builder.Default
    private Integer installments = 1;

    /** Bandeira do cartão (ex: visa, master, elo, amex) */
    private String paymentMethodId;

    /** ID do banco emissor (opcional — o MP extrai do CardToken) */
    private String issuerId;

    /** Descrição do pagamento */
    private String description;
}
