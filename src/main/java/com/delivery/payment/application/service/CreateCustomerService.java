package com.delivery.payment.application.service;

import com.delivery.payment.application.dto.request.CreateCustomerRequest;
import com.delivery.payment.application.usecase.CreateCustomerUseCase;
import com.delivery.payment.domain.customer.CreateCustomerCommand;
import com.delivery.payment.domain.customer.CustomerCardReference;
import com.delivery.payment.port.CustomerGatewayPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateCustomerService implements CreateCustomerUseCase {

    private final CustomerGatewayPort customerGateway;

    @Override
    public CustomerCardReference execute(CreateCustomerRequest request) {
        log.info("Criando customer e salvando cartão para email={}", request.getEmail());

        return customerGateway.createCustomerWithCard(
                CreateCustomerCommand.builder()
                        .email(request.getEmail())
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .documentType(request.getDocumentType())
                        .documentNumber(request.getDocumentNumber())
                        .cardToken(request.getCardToken())
                        .paymentMethodId(request.getPaymentMethodId())
                        .build());
    }
}
