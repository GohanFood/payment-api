package com.delivery.payment.domain.customer;

import lombok.Builder;
import lombok.Getter;

/**
 * Comando para adicionar/substituir um cartão em um Customer existente.
 */
@Getter
@Builder
public class AddCardCommand {

    /** CardToken gerado pelo MercadoPago.js (uso único). */
    private final String cardToken;

    /** Bandeira do cartão (visa, master, elo, amex) — opcional. */
    private final String paymentMethodId;
}
