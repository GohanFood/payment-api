package com.delivery.payment.domain.payment.gateway;

/**
 * Port para integração com gateways de pagamento externos (Mercado Pago, Stripe, etc.).
 */
public interface PaymentGatewayPort {

    /**
     * Processa um pagamento via cartão no gateway externo.
     *
     * @param request dados do pagamento (token do cartão, valor, parcelas, comprador, etc.)
     * @return resposta do gateway com ID externo e status da transação
     */
    PaymentGatewayResponse processCardPayment(PaymentGatewayRequest request);

    /**
     * Reembolsa um pagamento já processado no gateway externo.
     *
     * @param gatewayPaymentId ID do pagamento no gateway externo
     * @return resposta do reembolso
     */
    PaymentGatewayResponse refundPayment(String gatewayPaymentId);

    /**
     * Consulta o status atual de um pagamento no gateway externo.
     * Usado para sincronização via webhook (IPN do Mercado Pago).
     *
     * @param gatewayPaymentId ID do pagamento no gateway externo
     * @return status atual do pagamento no gateway
     */
    PaymentGatewayResponse getPayment(String gatewayPaymentId);
}
