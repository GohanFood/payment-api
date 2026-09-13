package com.delivery.payment.application.service;

import com.delivery.payment.application.dto.request.CreatePaymentRequest;
import com.delivery.payment.application.dto.response.PixPaymentResponse;
import com.delivery.payment.application.usecase.CreatePaymentUseCase;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
import com.delivery.payment.domain.payment.exception.GatewayUnavailableException;
import com.delivery.payment.domain.payment.exception.InvalidPaymentAmountException;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayPort;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayRequest;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayResponse;
import com.delivery.payment.port.PaymentMessagingPort;
import com.delivery.payment.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatePaymentService implements CreatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentMessagingPort paymentMessagingPort;
    private final com.delivery.payment.port.PaymentGatewayPort pixGateway;
    private final PaymentGatewayPort cardGateway;

    @Override
    @Transactional
    public Payment execute(CreatePaymentRequest request, String userId) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentAmountException(request.getAmount());
        }

        // The caller's reference identifies one business payment (for example, one
        // subscription cycle). Returning it makes retries safe even after a timeout.
        Payment existing = paymentRepository.findByReferenceId(request.getReferenceId()).stream()
                .filter(payment -> userId.equals(payment.getUserId()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            log.info("Pagamento idempotente reutilizado: paymentId={}, referenceId={}",
                    existing.getId(), request.getReferenceId());
            return existing;
        }

        Payment newPayment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .referenceId(request.getReferenceId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .payerEmail(request.getPayerEmail())
                .payerDocumentType(request.getPayerDocumentType())
                .payerDocumentNumber(request.getPayerDocumentNumber())
                .customerId(request.getCustomerId())
                .cardId(request.getCardId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Integração com Mercado Pago para PIX
        if (newPayment.isPix()) {
            processPixPayment(newPayment, request);
        }
        // Integração com Mercado Pago para Cartão (CREDIT_CARD, DEBIT_CARD)
        // via cardToken (cartão novo) ou cardId (cartão salvo/recorrência)
        else if (isCardPayment(request) && hasCardSource(request)) {
            processCardPayment(newPayment, request);
        }

        Payment saved = paymentRepository.save(newPayment);
        paymentMessagingPort.publishPaymentCreated(saved.getId(), saved.getReferenceId());
        return saved;
    }

    private boolean isCardPayment(CreatePaymentRequest request) {
        String method = request.getPaymentMethod();
        return "CREDIT_CARD".equalsIgnoreCase(method) || "DEBIT_CARD".equalsIgnoreCase(method);
    }

    private boolean hasCardSource(CreatePaymentRequest request) {
        return (request.getGatewayToken() != null && !request.getGatewayToken().isBlank())
                || (request.getCardId() != null && !request.getCardId().isBlank());
    }

    private void processPixPayment(Payment payment, CreatePaymentRequest request) {
        try {
            String idempotencyKey = "IDEMP-" + payment.getId();

            PixPaymentResponse mpResponse = pixGateway.createPixPayment(
                    idempotencyKey,
                    payment.getAmount(),
                    "Assinatura " + payment.getReferenceId(),
                    payment.getPayerEmail(),
                    "",
                    "",
                    payment.getPayerDocumentType(),
                    payment.getPayerDocumentNumber()
            );

            payment.linkMercadoPago(
                    mpResponse.getMpPaymentId(),
                    mpResponse.getQrCode(),
                    mpResponse.getQrCodeBase64(),
                    mpResponse.getTicketUrl(),
                    mpResponse.getDateOfExpiration() != null
                            ? mpResponse.getDateOfExpiration().toLocalDateTime()
                            : LocalDateTime.now().plusHours(24)
            );

            log.info("Pagamento PIX criado no MP: paymentId={}, mpPaymentId={}",
                    payment.getId(), mpResponse.getMpPaymentId());
        } catch (Exception e) {
            log.error("Falha ao integrar com Mercado Pago para paymentId={}: {}",
                    payment.getId(), e.getMessage(), e);
            throw new GatewayUnavailableException(
                    "Falha ao criar pagamento PIX no Mercado Pago: " + e.getMessage(), e);
        }
    }

    private void processCardPayment(Payment payment, CreatePaymentRequest request) {
        try {
            PaymentGatewayRequest gatewayRequest = PaymentGatewayRequest.builder()
                    .cardToken(request.getGatewayToken())
                    .cardId(request.getCardId())
                    .customerId(request.getCustomerId())
                    .transactionAmount(payment.getAmount())
                    .installments(request.getInstallments() != null ? request.getInstallments() : 1)
                    .paymentMethodId(request.getPaymentMethodId())
                    .issuerId(request.getIssuerId())
                    .description(request.getDescription() != null
                            ? request.getDescription()
                            : "Assinatura " + payment.getReferenceId())
                    .payerEmail(request.getPayerEmail())
                    .identificationType(request.getPayerDocumentType())
                    .identificationNumber(request.getPayerDocumentNumber())
                    .idempotencyKey(payment.getId().toString())
                    .build();

            PaymentGatewayResponse gatewayResponse = cardGateway.processCardPayment(gatewayRequest);

            if (gatewayResponse.isApproved()) {
                payment.markAsCompleted(gatewayResponse.getExternalId());
                log.info("Pagamento via cartão aprovado: paymentId={}, mpPaymentId={}",
                        payment.getId(), gatewayResponse.getExternalId());
            } else if (gatewayResponse.isRejected()) {
                payment.markAsFailed();
                log.warn("Pagamento via cartão rejeitado: paymentId={}, statusDetail={}",
                        payment.getId(), gatewayResponse.getExternalStatusDetail());
            } else {
                log.info("Pagamento via cartão pendente no gateway: paymentId={}, mpPaymentId={}, mpStatus={}",
                        payment.getId(), gatewayResponse.getExternalId(), gatewayResponse.getExternalStatus());
            }
        } catch (Exception e) {
            log.error("Falha ao processar cartão no Mercado Pago para paymentId={}: {}",
                    payment.getId(), e.getMessage(), e);
            throw new GatewayUnavailableException(
                    "Falha ao processar pagamento com cartão no Mercado Pago: " + e.getMessage(), e);
        }
    }
}
