package com.delivery.payment.domain.customer;

import lombok.Builder;
import lombok.Getter;

/**
 * Referência de cartão salvo no gateway. Contém apenas tokens — nunca dados
 * de cartão (número, CVV ou validade).
 */
@Getter
@Builder
public class CustomerCardReference {

    /** ID do Customer no Mercado Pago (token). */
    private final String customerId;

    /** ID do cartão salvo no Mercado Pago (token). */
    private final String cardId;

    /** Bandeira do cartão (visa, master, elo, amex). */
    private final String paymentMethodId;
}
