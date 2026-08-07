package com.delivery.payment.port;

import com.delivery.payment.application.dto.response.PixPaymentResponse;

import java.math.BigDecimal;

public interface PaymentGatewayPort {

    /**
     * Cria um pagamento PIX no Mercado Pago.
     *
     * @param idempotencyKey  chave de idempotência para evitar duplicidade
     * @param amount          valor do pagamento
     * @param description     descrição do pagamento
     * @param payerEmail      email do pagador
     * @param payerFirstName  nome do pagador
     * @param payerLastName   sobrenome do pagador
     * @param documentType    tipo de documento (CPF, CNPJ)
     * @param documentNumber  número do documento
     * @return resposta com dados do QR Code PIX
     */
    PixPaymentResponse createPixPayment(
            String idempotencyKey,
            BigDecimal amount,
            String description,
            String payerEmail,
            String payerFirstName,
            String payerLastName,
            String documentType,
            String documentNumber);
}
