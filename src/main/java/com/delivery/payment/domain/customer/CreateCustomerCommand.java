package com.delivery.payment.domain.customer;

import lombok.Builder;
import lombok.Getter;

/**
 * Comando para criar um Customer no gateway e associar um cartão a partir de
 * um {@code cardToken}.
 */
@Getter
@Builder
public class CreateCustomerCommand {

    /** Email do cliente (obrigatório para o Mercado Pago). */
    private final String email;

    /** Nome do cliente (opcional). */
    private final String firstName;

    /** Sobrenome do cliente (opcional). */
    private final String lastName;

    /** Tipo de documento (CPF, CNPJ) — opcional. */
    private final String documentType;

    /** Número do documento — opcional. */
    private final String documentNumber;

    /** CardToken gerado pelo MercadoPago.js (uso único). */
    private final String cardToken;

    /** Bandeira do cartão (visa, master, elo, amex) — opcional. */
    private final String paymentMethodId;
}
