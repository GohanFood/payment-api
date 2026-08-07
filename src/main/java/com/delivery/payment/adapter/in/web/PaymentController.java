package com.delivery.payment.adapter.in.web;

import com.delivery.payment.application.dto.request.CreatePaymentRequest;
import com.delivery.payment.application.dto.request.ProcessPaymentRequest;
import com.delivery.payment.application.dto.request.RefundPaymentRequest;
import com.delivery.payment.application.dto.response.PaymentResponse;
import com.delivery.payment.application.usecase.*;
import com.delivery.payment.application.service.PaymentStatusSyncService;
import com.delivery.payment.domain.payment.Payment;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = Payment.builder()
                .userId(request.getUserId())
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .payerEmail(request.getPayerEmail())
                .payerDocumentType(request.getPayerDocumentType())
                .payerDocumentNumber(request.getPayerDocumentNumber())
                .build();

        Payment created = createPaymentUseCase.execute(payment);
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
    public ResponseEntity<List<PaymentResponse>> listByUser(@RequestParam UUID userId) {
        List<Payment> payments = listPaymentsUseCase.execute(userId);
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
     * Formato esperado do body (Mercado Pago):
     * {
     *   "id": 123456789,
     *   "type": "payment",
     *   "action": "payment.updated",
     *   "data": { "id": "123456789" }
     * }
     *
     * @see <a href="https://www.mercadopago.com.br/developers/pt/docs/checkout-api-payments/additional-content/your-integrations/notifications">Notificações Mercado Pago</a>
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody Map<String, Object> payload) {
        log.info("Webhook Mercado Pago recebido: {}", payload);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            if (data != null && data.get("id") != null) {
                String mpPaymentId = data.get("id").toString();
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

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .userId(payment.getUserId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .mpPaymentId(payment.getMpPaymentId())
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
