package com.delivery.payment.domain.payment.gateway;

import lombok.Builder;
import lombok.Getter;

/**
 * Value Object representando a resposta do gateway de pagamento.
 */
@Getter
@Builder
public class PaymentGatewayResponse {

    /** ID do pagamento no gateway externo (Mercado Pago) */
    private final String externalId;

    /** Status bruto retornado pelo gateway: approved, rejected, in_process, pending, refunded */
    private final String externalStatus;

    /** Detalhe do status (ex: accredited, cc_rejected_insufficient_amount) */
    private final String externalStatusDetail;

    /** Tipo de pagamento (ex: credit_card, debit_card) */
    private final String paymentTypeId;

    /** Data de aprovação no gateway (ISO-8601) */
    private final String dateApproved;

    /** Método de pagamento usado (ex: visa, master) */
    private final String paymentMethodId;

    /** Número de parcelas */
    private final Integer installments;

    public boolean isApproved() {
        return "approved".equalsIgnoreCase(externalStatus);
    }

    public boolean isRejected() {
        return "rejected".equalsIgnoreCase(externalStatus);
    }

    public boolean isPending() {
        return "pending".equalsIgnoreCase(externalStatus) || "in_process".equalsIgnoreCase(externalStatus);
    }

    public boolean isRefunded() {
        return "refunded".equalsIgnoreCase(externalStatus);
    }
}
