package com.delivery.payment.application.service;

import com.delivery.payment.application.usecase.DeleteCardUseCase;
import com.delivery.payment.port.CustomerGatewayPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteCardService implements DeleteCardUseCase {

    private final CustomerGatewayPort customerGateway;

    @Override
    public void execute(String customerId, String cardId) {
        log.info("Removendo cartão customerId={}, cardId={}", customerId, cardId);
        customerGateway.deleteCard(customerId, cardId);
    }
}
