package com.delivery.payment.port;

import com.delivery.payment.domain.customer.AddCardCommand;
import com.delivery.payment.domain.customer.CreateCustomerCommand;
import com.delivery.payment.domain.customer.CustomerCardReference;

/**
 * Porta de saída para operações de Customer + Card (cartão salvo) no gateway.
 */
public interface CustomerGatewayPort {

    /**
     * Cria um Customer e associa um cartão a partir de um {@code cardToken}.
     *
     * @return referência com {@code customerId} e {@code cardId} (tokens)
     */
    CustomerCardReference createCustomerWithCard(CreateCustomerCommand command);

    /**
     * Adiciona/substitui o cartão de um Customer existente.
     *
     * @return referência com o novo {@code cardId}
     */
    CustomerCardReference addCard(String customerId, AddCardCommand command);

    /**
     * Remove um cartão salvo de um Customer.
     */
    void deleteCard(String customerId, String cardId);
}
