package com.delivery.payment.application.service;

import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayPort;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayResponse;
import com.delivery.payment.port.PaymentMessagingPort;
import com.delivery.payment.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Serviço para sincronizar status de pagamentos a partir de notificações
 * do Mercado Pago (IPN - Instant Payment Notification).
 *
 * Quando o Mercado Pago processa um pagamento (cartão ou PIX),
 * ele envia um webhook POST para /api/v1/payments/webhook
 * com o ID do pagamento no gateway (data.id).
 *
 * Fluxo:
 * 1. Mercado Pago envia webhook com data.id = 123456789
 * 2. Buscamos o pagamento local pelo gatewayTransactionId = "123456789"
 * 3. Consultamos o status atual no Mercado Pago via GET /v1/payments/{id}
 * 4. Atualizamos o status local conforme resposta do MP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentStatusSyncService {

    private final PaymentRepository paymentRepository;
    private final PaymentMessagingPort paymentMessagingPort;
    private final PaymentGatewayPort paymentGateway;

    /**
     * Processa notificação de webhook do Mercado Pago.
     *
     * @param mpPaymentId ID do pagamento no Mercado Pago (data.id do webhook)
     */
    @Transactional
    public void handleWebhookNotification(String mpPaymentId) {
        log.info("Processando webhook do Mercado Pago: mpPaymentId={}", mpPaymentId);

        // Busca pagamento local pelo gatewayTransactionId (armazena o ID do MP)
        Optional<Payment> optPayment = paymentRepository.findByGatewayTransactionId(mpPaymentId);

        if (optPayment.isEmpty()) {
            log.warn("Nenhum pagamento local encontrado para gatewayTransactionId={}", mpPaymentId);
            return;
        }

        Payment payment = optPayment.get();

        // Se já está em estado final, ignora
        if (isFinalStatus(payment.getStatus())) {
            log.info("Pagamento já em estado final: paymentId={}, status={}",
                    payment.getId(), payment.getStatus());
            return;
        }

        // Consulta status atual no Mercado Pago
        PaymentGatewayResponse mpStatus = paymentGateway.getPayment(mpPaymentId);

        log.info("Status MP: paymentId={}, mpStatus={}, mpStatusDetail={}",
                payment.getId(), mpStatus.getExternalStatus(), mpStatus.getExternalStatusDetail());

        // Atualiza status local
        if (mpStatus.isApproved()) {
            // Se ainda não tem gatewayTransactionId, associa
            if (payment.getGatewayTransactionId() == null) {
                payment.markAsCompleted(mpPaymentId);
            }
            Payment updated = paymentRepository.save(payment);
            paymentMessagingPort.publishPaymentCompleted(updated.getId(), updated.getOrderId());
            log.info("Pagamento aprovado via webhook: paymentId={}", payment.getId());

        } else if (mpStatus.isRejected()) {
            payment.markAsFailed();
            paymentRepository.save(payment);
            log.warn("Pagamento rejeitado via webhook: paymentId={}, detail={}",
                    payment.getId(), mpStatus.getExternalStatusDetail());

        } else {
            log.info("Pagamento ainda pendente no MP: paymentId={}, mpStatus={}",
                    payment.getId(), mpStatus.getExternalStatus());
        }
    }

    private boolean isFinalStatus(PaymentStatus status) {
        return status == PaymentStatus.COMPLETED
                || status == PaymentStatus.REFUNDED
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.CANCELLED;
    }
}
