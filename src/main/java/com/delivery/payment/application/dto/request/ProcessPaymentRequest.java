package com.delivery.payment.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Requisição para processar um pagamento via gateway.
 *
 * Campos obrigatórios para cartão (Mercado Pago):
 * - gatewayToken: CardToken gerado pelo MercadoPago.js
 * - payerEmail: email do comprador
 *
 * Campos opcionais (recomendados para melhor taxa de aprovação):
 * - installments: número de parcelas (default: 1)
 * - paymentMethodId: bandeira do cartão (ex: visa, master)
 * - issuerId: ID do banco emissor
 * - identificationType: tipo de documento (CPF, CNPJ)
 * - identificationNumber: número do documento
 * - description: descrição do pagamento
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequest {

    /** CardToken gerado pelo MercadoPago.js CardForm (obrigatório) */
    @NotNull
    private String gatewayToken;

    /** Email do comprador (obrigatório para Mercado Pago) */
    @NotNull
    private String payerEmail;

    /** Número de parcelas (default: 1) */
    @Builder.Default
    private Integer installments = 1;

    /** ID do método de pagamento (ex: visa, master, elo, amex) */
    private String paymentMethodId;

    /** ID do banco emissor */
    private String issuerId;

    /** Tipo de documento do comprador (ex: CPF, CNPJ) */
    private String identificationType;

    /** Número do documento do comprador */
    private String identificationNumber;

    /** Descrição do pagamento */
    private String description;
}
