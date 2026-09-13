package com.delivery.payment.application.service;

import com.delivery.payment.application.dto.request.AddCardRequest;
import com.delivery.payment.application.usecase.AddCardUseCase;
import com.delivery.payment.domain.customer.AddCardCommand;
import com.delivery.payment.domain.customer.CustomerCardReference;
import com.delivery.payment.port.CustomerGatewayPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddCardService implements AddCardUseCase {

    private final CustomerGatewayPort customerGateway;

    @Override
    public CustomerCardReference execute(String customerId, AddCardRequest request) {
        log.info("Adicionando/trocando cartão para customerId={}", customerId);

        return customerGateway.addCard(
                customerId,
                AddCardCommand.builder()
                        .cardToken(request.getCardToken())
                        .paymentMethodId(request.getPaymentMethodId())
                        .build());
    }
}
