package com.delivery.payment.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Validador de assinatura de webhooks do Mercado Pago.
 *
 * <h3>Como funciona a validação:</h3>
 * O Mercado Pago envia dois headers em cada notificação de webhook:
 * <ul>
 *   <li>{@code x-signature}: assinatura HMAC-SHA256 do payload no formato {@code ts=1700000000,v1=abcd1234}</li>
 *   <li>{@code x-request-id}: ID único da requisição (opcional, usado para idempotência)</li>
 * </ul>
 *
 * A validação segue o <a href="https://www.mercadopago.com.br/developers/pt/docs/your-integrations/notifications/webhooks#editor_6">guia oficial do Mercado Pago</a>:
 * <ol>
 *   <li>Extrai {@code ts} e {@code v1} do header {@code x-signature}</li>
 *   <li>Constrói o {@code signed_payload = "id:{data.id};request-id:{x-request-id};ts:{ts};data={body}"}</li>
 *   <li>Calcula HMAC-SHA256 do {@code signed_payload} usando o {@code webhookSecret} como chave</li>
 *   <li>Compara o resultado (hex) com {@code v1}</li>
 * </ol>
 *
 * @see <a href="https://www.mercadopago.com.br/developers/pt/docs/your-integrations/notifications/webhooks">Documentação de Webhooks</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MercadoPagoWebhookValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "v1=";
    private static final String TS_PREFIX = "ts=";

    private final MercadoPagoProperties properties;

    /**
     * Valida a assinatura de uma notificação de webhook do Mercado Pago.
     *
     * @param payload     corpo da requisição (raw JSON string)
     * @param xSignature  valor do header {@code x-signature}
     * @param xRequestId  valor do header {@code x-request-id} (pode ser null)
     * @param dataId      valor de {@code data.id} extraído do payload
     * @return {@code true} se a assinatura é válida ou se a validação está desabilitada
     */
    public boolean isValid(String payload, String xSignature, String xRequestId, String dataId) {
        // Se webhook secret não está configurado, desabilita validação (dev/test)
        if (properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank()) {
            log.debug("Validação de webhook desabilitada: webhook-secret não configurado");
            return true;
        }

        if (xSignature == null || xSignature.isBlank()) {
            log.warn("Header x-signature ausente no webhook");
            return false;
        }

        if (dataId == null || dataId.isBlank()) {
            log.warn("data.id ausente no payload do webhook");
            return false;
        }

        try {
            // Extrai ts e v1 do formato "ts=1700000000,v1=abcd1234"
            String ts = extractParam(xSignature, TS_PREFIX);
            String signature = extractParam(xSignature, SIGNATURE_PREFIX);

            if (ts == null || signature == null) {
                log.warn("Formato inválido do header x-signature: {}", xSignature);
                return false;
            }

            // Constrói o signed_payload conforme especificação do Mercado Pago
            String signedPayload = buildSignedPayload(dataId, xRequestId, ts, payload);

            // Calcula HMAC-SHA256 e compara
            String computedSignature = computeHmac(signedPayload, properties.getWebhookSecret());

            boolean valid = computedSignature.equalsIgnoreCase(signature);
            if (!valid) {
                log.warn("Assinatura de webhook inválida: esperada={}, recebida={}", computedSignature, signature);
            }
            return valid;

        } catch (Exception e) {
            log.error("Erro ao validar assinatura do webhook", e);
            return false;
        }
    }

    /**
     * Constrói o payload assinado conforme especificação do Mercado Pago.
     *
     * <p>Formato:
     * {@code id:{data.id};request-id:{x-request-id};ts:{timestamp};data={raw_body}}
     */
    String buildSignedPayload(String dataId, String xRequestId, String ts, String rawBody) {
        StringBuilder sb = new StringBuilder();
        sb.append("id:").append(dataId).append(";");
        sb.append("request-id:").append(xRequestId != null ? xRequestId : "").append(";");
        sb.append("ts:").append(ts).append(";");
        sb.append("data:").append(rawBody);
        return sb.toString();
    }

    /**
     * Extrai o valor de um parâmetro do header x-signature.
     * Ex: para a string "ts=1700000000,v1=abcd1234" com prefix "ts=" retorna "1700000000".
     */
    private String extractParam(String header, String prefix) {
        int start = header.indexOf(prefix);
        if (start == -1) return null;
        start += prefix.length();
        int end = header.indexOf(',', start);
        return end == -1 ? header.substring(start) : header.substring(start, end);
    }

    /**
     * Calcula HMAC-SHA256 de uma string e retorna o resultado em hexadecimal minúsculo.
     */
    private String computeHmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao calcular HMAC-SHA256", e);
        }
    }
}
