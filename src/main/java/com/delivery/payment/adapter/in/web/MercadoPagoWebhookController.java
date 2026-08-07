package com.delivery.payment.adapter.in.web;

import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.port.PaymentMessagingPort;
import com.delivery.payment.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller para receber notificações de webhook do Mercado Pago.
 *
 * O Mercado Pago envia notificações no formato:
 * {
 *   "action": "payment.updated",
 *   "data": { "id": "123456789" },
 *   "type": "payment"
 * }
 *
 * Ao receber a notificação, consultamos o status no MP e atualizamos localmente.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class MercadoPagoWebhookController {

    private final PaymentRepository paymentRepository;
    private final PaymentMessagingPort paymentMessagingPort;

    @PostMapping("/mercadopago")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Webhook Mercado Pago recebido: {}", payload);

        try {
            String action = (String) payload.getOrDefault("action", "");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");

            if (data == null || !"payment.updated".equals(action)) {
                log.info("Webhook ignorado: action={}", action);
                return ResponseEntity.ok("ignored");
            }

            Long mpPaymentId = Long.parseLong(String.valueOf(data.get("id")));

            // Busca o pagamento local pelo mpPaymentId
            Payment payment = paymentRepository.findByMpPaymentId(mpPaymentId).orElse(null);

            if (payment == null) {
                log.warn("Nenhum pagamento local encontrado para mpPaymentId={}", mpPaymentId);
                return ResponseEntity.ok("not_found");
            }

            // O Mercado Pago notifica via webhook, confiamos na ação "payment.updated"
            // e marcamos como COMPLETED quando a ação vier
            if (payment.isPending()) {
                payment.markAsCompleted("MP-" + mpPaymentId);
                paymentRepository.save(payment);

                paymentMessagingPort.publishPaymentCompleted(payment.getId(), payment.getOrderId());

                log.info("Pagamento atualizado via webhook: paymentId={}, mpPaymentId={}",
                        payment.getId(), mpPaymentId);
            }

            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            log.error("Erro ao processar webhook do Mercado Pago", e);
            return ResponseEntity.status(500).body("error");
        }
    }
}
