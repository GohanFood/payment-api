package com.delivery.payment.application.service;

import com.delivery.payment.application.dto.request.ProcessPaymentRequest;
import com.delivery.payment.application.usecase.ProcessPaymentUseCase;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
import com.delivery.payment.domain.payment.exception.PaymentAlreadyProcessedException;
import com.delivery.payment.domain.payment.exception.PaymentNotFoundException;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayPort;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayRequest;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayResponse;
import com.delivery.payment.port.PaymentMessagingPort;
import com.delivery.payment.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentService implements ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentMessagingPort paymentMessagingPort;
    private final PaymentGatewayPort paymentGateway;

    @Override
    @Transactional
    public Payment execute(UUID paymentId, ProcessPaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentAlreadyProcessedException(paymentId);
        }

        // Monta requisição para o gateway
        PaymentGatewayRequest gatewayRequest = PaymentGatewayRequest.builder()
                .cardToken(request.getGatewayToken())
                .transactionAmount(payment.getAmount())
                .installments(request.getInstallments())
                .paymentMethodId(request.getPaymentMethodId())
                .issuerId(request.getIssuerId())
                .description(request.getDescription() != null
                        ? request.getDescription()
                        : "Assinatura " + payment.getReferenceId())
                .payerEmail(request.getPayerEmail())
                .identificationType(request.getIdentificationType())
                .identificationNumber(request.getIdentificationNumber())
                .idempotencyKey(payment.getId().toString())
                .build();

        // Processa via gateway Mercado Pago
        PaymentGatewayResponse gatewayResponse = paymentGateway.processCardPayment(gatewayRequest);

        // Mapeia resposta do gateway para o estado do domínio
        if (gatewayResponse.isApproved()) {
            payment.markAsCompleted(gatewayResponse.getExternalId());
            Payment updated = paymentRepository.save(payment);
            paymentMessagingPort.publishPaymentCompleted(updated.getId(), updated.getReferenceId());
            log.info("Pagamento aprovado: paymentId={}, mpPaymentId={}",
                    paymentId, gatewayResponse.getExternalId());
            return updated;
        } else if (gatewayResponse.isRejected()) {
            payment.markAsFailed();
            Payment updated = paymentRepository.save(payment);
            log.warn("Pagamento rejeitado: paymentId={}, statusDetail={}",
                    paymentId, gatewayResponse.getExternalStatusDetail());
            return updated;
        } else {
            // pending ou in_process — salva o externalId mas mantém PENDING
            // O status será atualizado via webhook de notificação do Mercado Pago
            log.info("Pagamento pendente no gateway: paymentId={}, mpPaymentId={}, mpStatus={}",
                    paymentId, gatewayResponse.getExternalId(), gatewayResponse.getExternalStatus());
            return payment;
        }
    }
}
