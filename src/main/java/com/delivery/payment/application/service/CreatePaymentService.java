package com.delivery.payment.application.service;

import com.delivery.payment.application.dto.response.PixPaymentResponse;
import com.delivery.payment.application.usecase.CreatePaymentUseCase;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
import com.delivery.payment.domain.payment.exception.GatewayUnavailableException;
import com.delivery.payment.domain.payment.exception.InvalidPaymentAmountException;
import com.delivery.payment.port.PaymentGatewayPort;
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
    private final PaymentGatewayPort paymentGatewayPort;

    @Override
    @Transactional
    public Payment execute(Payment payment) {
        if (payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentAmountException(payment.getAmount());
        }

        Payment newPayment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(payment.getUserId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .payerEmail(payment.getPayerEmail())
                .payerDocumentType(payment.getPayerDocumentType())
                .payerDocumentNumber(payment.getPayerDocumentNumber())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Integração com Mercado Pago para PIX
        if (newPayment.isPix()) {
            try {
                String idempotencyKey = "IDEMP-" + newPayment.getId();

                PixPaymentResponse mpResponse = paymentGatewayPort.createPixPayment(
                        idempotencyKey,
                        newPayment.getAmount(),
                        "Pedido #" + newPayment.getOrderId(),
                        newPayment.getPayerEmail(),
                        payment.getPayerEmail(), // payerFirstName será usado o email como fallback
                        "",
                        newPayment.getPayerDocumentType(),
                        newPayment.getPayerDocumentNumber()
                );

                newPayment.linkMercadoPago(
                        mpResponse.getMpPaymentId(),
                        mpResponse.getQrCode(),
                        mpResponse.getQrCodeBase64(),
                        mpResponse.getTicketUrl(),
                        mpResponse.getDateOfExpiration() != null
                                ? mpResponse.getDateOfExpiration().toLocalDateTime()
                                : LocalDateTime.now().plusHours(24)
                );

                log.info("Pagamento PIX criado no MP: paymentId={}, mpPaymentId={}",
                        newPayment.getId(), mpResponse.getMpPaymentId());
            } catch (Exception e) {
                log.error("Falha ao integrar com Mercado Pago para paymentId={}: {}",
                        newPayment.getId(), e.getMessage(), e);
                throw new GatewayUnavailableException(
                        "Falha ao criar pagamento PIX no Mercado Pago: " + e.getMessage(), e);
            }
        }

        Payment saved = paymentRepository.save(newPayment);

        paymentMessagingPort.publishPaymentCreated(saved.getId(), saved.getOrderId());

        return saved;
    }
}
