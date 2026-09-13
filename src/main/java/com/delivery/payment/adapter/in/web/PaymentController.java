package com.delivery.payment.adapter.in.web;

import com.delivery.payment.application.dto.request.CreatePaymentRequest;
import com.delivery.payment.application.dto.request.ProcessPaymentRequest;
import com.delivery.payment.application.dto.request.RefundPaymentRequest;
import com.delivery.payment.application.dto.response.PaymentResponse;
import com.delivery.payment.application.usecase.*;
import com.delivery.payment.application.service.PaymentStatusSyncService;
import com.delivery.payment.config.MercadoPagoWebhookValidator;
import com.delivery.payment.domain.payment.Payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;
    private final ProcessPaymentUseCase processPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final ListPaymentsUseCase listPaymentsUseCase;
    private final RefundPaymentUseCase refundPaymentUseCase;
    private final PaymentStatusSyncService paymentStatusSyncService;
    private final MercadoPagoWebhookValidator webhookValidator;
    private final ObjectMapper objectMapper;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest request) {
        String userId = getCurrentUserId();
        Payment created = createPaymentUseCase.execute(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> process(@PathVariable UUID id,
                                                    @Valid @RequestBody ProcessPaymentRequest request) {
        Payment processed = processPaymentUseCase.execute(id, request);
        return ResponseEntity.ok(toResponse(processed));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> getById(@PathVariable UUID id) {
        Payment payment = getPaymentUseCase.execute(id);
        return ResponseEntity.ok(toResponse(payment));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PaymentResponse>> listByUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = getCurrentUserId();
        List<Payment> payments = listPaymentsUseCase.execute(userId, page, size);
        return ResponseEntity.ok(payments.stream().map(this::toResponse).toList());
    }

    @PostMapping("/refund")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> refund(@Valid @RequestBody RefundPaymentRequest request) {
        Payment refunded = refundPaymentUseCase.execute(request.getPaymentId());
        return ResponseEntity.ok(toResponse(refunded));
    }

    /**
     * Webhook para notificações do Mercado Pago (IPN - Instant Payment Notification).
     * Endpoint público — o Mercado Pago envia notificações de mudança de status.
     *
     * <h3>Headers esperados:</h3>
     * <ul>
     *   <li>{@code x-signature}: assinatura HMAC-SHA256 (formato: {@code ts=...,v1=...})</li>
     *   <li>{@code x-request-id}: ID único da requisição (opcional)</li>
     * </ul>
     *
     * <h3>Formato esperado do body:</h3>
     * <pre>{@code
     * {
     *   "id": 123456789,
     *   "type": "payment",
     *   "action": "payment.updated",
     *   "data": { "id": "123456789" }
     * }
     * }</pre>
     *
     * @see <a href="https://www.mercadopago.com.br/developers/pt/docs/your-integrations/notifications/webhooks">Validação de Webhooks</a>
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId) {

        log.info("Webhook Mercado Pago recebido: x-request-id={}", xRequestId);

        try {
            Map<String, Object> payload = objectMapper.readValue(rawPayload, Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            String mpPaymentId = data != null && data.get("id") != null
                    ? data.get("id").toString() : null;

            // Valida assinatura do webhook conforme especificação do Mercado Pago
            if (!webhookValidator.isValid(rawPayload, xSignature, xRequestId, mpPaymentId)) {
                log.warn("Webhook com assinatura inválida: x-request-id={}", xRequestId);
                return ResponseEntity.ok().build();
            }

            if (mpPaymentId != null) {
                paymentStatusSyncService.handleWebhookNotification(mpPaymentId);
            } else {
                log.warn("Webhook sem data.id: {}", payload);
            }
        } catch (Exception e) {
            log.error("Erro ao processar webhook do Mercado Pago", e);
        }

        // Sempre retorna 200 OK para o Mercado Pago não reenviar
        return ResponseEntity.ok().build();
    }

    /**
     * Extrai o userId do token JWT atual (campo "sub").
     */
    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Usuário não autenticado");
        }
        return auth.getName();
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .userId(payment.getUserId())
                .referenceId(payment.getReferenceId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .mpPaymentId(payment.getMpPaymentId())
                .customerId(payment.getCustomerId())
                .cardId(payment.getCardId())
                .qrCode(payment.getQrCode())
                .qrCodeBase64(payment.getQrCodeBase64())
                .ticketUrl(payment.getTicketUrl())
                .payerEmail(payment.getPayerEmail())
                .payerDocumentType(payment.getPayerDocumentType())
                .payerDocumentNumber(payment.getPayerDocumentNumber())
                .expiresAt(payment.getExpiresAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
