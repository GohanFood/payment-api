package com.delivery.payment.domain.payment.gateway;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Value Object representando a requisição enviada ao gateway de pagamento.
 */
@Getter
@Builder
public class PaymentGatewayRequest {

    /** Token do cartão gerado pelo MercadoPago.js (CardToken) */
    private final String cardToken;

    /** Valor da transação */
    private final BigDecimal transactionAmount;

    /** Método de pagamento (ex: visa, master, elo, amex) */
    private final String paymentMethodId;

    /** ID do banco emissor */
    private final String issuerId;

    /** Número de parcelas */
    private final Integer installments;

    /** Descrição do produto/pedido */
    private final String description;

    /** Email do comprador (obrigatório para Mercado Pago) */
    private final String payerEmail;

    /** Tipo de documento do comprador (CPF, CNPJ) */
    private final String identificationType;

    /** Número do documento do comprador */
    private final String identificationNumber;

    /** ID externo do pagamento (nosso paymentId) para idempotência */
    private final String idempotencyKey;
}
