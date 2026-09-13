package com.delivery.payment.application.service;

import com.delivery.payment.application.dto.request.AddCardRequest;
import com.delivery.payment.application.dto.request.CreateCustomerRequest;
import com.delivery.payment.domain.customer.CreateCustomerCommand;
import com.delivery.payment.domain.customer.CustomerCardReference;
import com.delivery.payment.port.CustomerGatewayPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerGatewayPort customerGateway;

    @InjectMocks
    private CreateCustomerService createCustomerService;

    @InjectMocks
    private AddCardService addCardService;

    @InjectMocks
    private DeleteCardService deleteCardService;

    @Test
    void shouldCreateCustomerWithCard() {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .email("cliente@email.com")
                .firstName("Nome")
                .lastName("Sobrenome")
                .documentType("CPF")
                .documentNumber("19119119100")
                .cardToken("card-token")
                .paymentMethodId("master")
                .build();

        when(customerGateway.createCustomerWithCard(any(CreateCustomerCommand.class)))
                .thenReturn(CustomerCardReference.builder()
                        .customerId("customer-1")
                        .cardId("card-1")
                        .paymentMethodId("master")
                        .build());

        CustomerCardReference result = createCustomerService.execute(request);

        assertEquals("customer-1", result.getCustomerId());
        assertEquals("card-1", result.getCardId());
        assertEquals("master", result.getPaymentMethodId());

        ArgumentCaptor<CreateCustomerCommand> captor = ArgumentCaptor.forClass(CreateCustomerCommand.class);
        verify(customerGateway).createCustomerWithCard(captor.capture());
        assertEquals("cliente@email.com", captor.getValue().getEmail());
        assertEquals("card-token", captor.getValue().getCardToken());
    }

    @Test
    void shouldAddCardToCustomer() {
        AddCardRequest request = AddCardRequest.builder()
                .cardToken("new-card-token")
                .paymentMethodId("visa")
                .build();

        when(customerGateway.addCard(eq("customer-1"), any()))
                .thenReturn(CustomerCardReference.builder()
                        .customerId("customer-1")
                        .cardId("card-2")
                        .paymentMethodId("visa")
                        .build());

        CustomerCardReference result = addCardService.execute("customer-1", request);

        assertEquals("card-2", result.getCardId());
        assertEquals("visa", result.getPaymentMethodId());
    }

    @Test
    void shouldDeleteCard() {
        deleteCardService.execute("customer-1", "card-1");

        verify(customerGateway).deleteCard("customer-1", "card-1");
    }
}
