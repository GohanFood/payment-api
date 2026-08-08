package com.delivery.payment.adapter.out.gateway;

import com.delivery.payment.application.dto.response.PixPaymentResponse;
import com.delivery.payment.config.MercadoPagoProperties;
import com.delivery.payment.domain.payment.exception.GatewayUnavailableException;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayRequest;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayResponse;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.core.MPRequestOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Adapter que implementa a integração com a API de pagamentos do Mercado Pago
 * para PIX (QR Code) e Cartão de Crédito/Débito (CardForm + CardToken) —
 * exclusivamente no ambiente <b>SANDBOX</b>.
 *
 * <h3>Fluxo Cartão:</h3>
 * <ol>
 *   <li>Frontend captura dados do cartão via MercadoPago.js CardForm → gera CardToken</li>
 *   <li>Frontend envia CardToken + dados do comprador para nosso backend</li>
 *   <li>Backend chama processCardPayment() → Mercado Pago Sandbox API POST /v1/payments</li>
 *   <li>Mercado Pago retorna status: approved, rejected, in_process, pending</li>
 * </ol>
 *
 * <h3>Fluxo PIX:</h3>
 * <ol>
 *   <li>Backend chama createPixPayment() → Mercado Pago Sandbox API POST /v1/payments</li>
 *   <li>Mercado Pago retorna QR Code (base64 + texto) e ticket_url</li>
 *   <li>Frontend exibe QR Code para o comprador pagar</li>
 * </ol>
 *
 * <h3>Credenciais:</h3>
 * <p>Tokens de produção ({@code APP_USR-}) são <b>rejeitados</b> na inicialização.
 * Use tokens sandbox com prefixo {@code TEST-} obtidos em:
 * <a href="https://www.mercadopago.com.br/settings/account/credentials">Mercado Pago Credentials</a></p>
 *
 * @see <a href="https://www.mercadopago.com.br/developers/pt/docs/checkout-api-payments/integration-configuration/card/integrate-via-cardform">Cartão via CardForm</a>
 * @see <a href="https://www.mercadopago.com.br/developers/pt/reference/payments/_payments/post">API Reference</a>
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class MercadoPagoGateway
        implements com.delivery.payment.port.PaymentGatewayPort,
                   com.delivery.payment.domain.payment.gateway.PaymentGatewayPort {

    private static final String TOKEN_PREFIX_SANDBOX = "TEST-";
    private static final String TOKEN_PREFIX_PRODUCTION = "APP_USR-";

    private final MercadoPagoProperties properties;
    private boolean ready = false;

    @PostConstruct
    public void init() {
        if (!properties.isTokenConfigured()) {
            log.warn("============================================================");
            log.warn("  Mercado Pago Sandbox: access token NÃO configurado!");
            log.warn("  Configure a env var MERCADOPAGO_ACCESS_TOKEN com um");
            log.warn("  token de sandbox (prefixo {}).", TOKEN_PREFIX_SANDBOX);
            log.warn("  Obtenha suas credenciais em:");
            log.warn("  https://www.mercadopago.com.br/settings/account/credentials");
            log.warn("  Enquanto isso, chamadas ao gateway lançarão exceção.");
            log.warn("============================================================");
            return;
        }

        if (properties.isProductionToken()) {
            log.error("============================================================");
            log.error("  TOKEN DE PRODUÇÃO DETECTADO — API configurada só para SANDBOX!");
            log.error("  O token '{}' começa com '{}' (produção).",
                    maskToken(properties.getAccessToken()), TOKEN_PREFIX_PRODUCTION);
            log.error("  Substitua por um token sandbox (prefixo {}).", TOKEN_PREFIX_SANDBOX);
            log.error("============================================================");
            throw new IllegalStateException(
                    "Token de produção detectado. Esta API só aceita sandbox (TEST-).");
        }

        if (!properties.isSandboxToken()) {
            log.warn("Token Mercado Pago não parece ser de sandbox (esperado prefixo '{}').", TOKEN_PREFIX_SANDBOX);
        }

        MercadoPagoConfig.setAccessToken(properties.getAccessToken());

        ready = true;
        log.info("Mercado Pago Sandbox inicializado com sucesso. Token: {}", maskToken(properties.getAccessToken()));
    }

    // ──────────────────────────────────────────────
    //  PIX (port.PaymentGatewayPort)
    // ──────────────────────────────────────────────

    @Override
    public PixPaymentResponse createPixPayment(
            String idempotencyKey,
            BigDecimal amount,
            String description,
            String payerEmail,
            String payerFirstName,
            String payerLastName,
            String documentType,
            String documentNumber) {

        assertReady();

        log.info("Criando pagamento PIX no Mercado Pago Sandbox: amount={}, email={}", amount, payerEmail);

        try {
            PaymentClient client = new PaymentClient();

            PaymentCreateRequest createRequest = PaymentCreateRequest.builder()
                    .transactionAmount(amount)
                    .description(description)
                    .paymentMethodId("pix")
                    .payer(PaymentPayerRequest.builder()
                            .email(payerEmail)
                            .firstName(payerFirstName)
                            .lastName(payerLastName)
                            .identification(IdentificationRequest.builder()
                                    .type(documentType)
                                    .number(documentNumber)
                                    .build())
                            .build())
                    .build();

            MPRequestOptions requestOptions = MPRequestOptions.builder()
                    .customHeaders(Map.of("x-idempotency-key", idempotencyKey))
                    .build();

            com.mercadopago.resources.payment.Payment mpPayment = client.create(createRequest, requestOptions);

            log.info("Pagamento PIX criado no Mercado Pago Sandbox: mpPaymentId={}, status={}",
                    mpPayment.getId(), mpPayment.getStatus());

            String qrCode = null;
            String qrCodeBase64 = null;
            String ticketUrl = null;

            if (mpPayment.getPointOfInteraction() != null
                    && mpPayment.getPointOfInteraction().getTransactionData() != null) {
                var txData = mpPayment.getPointOfInteraction().getTransactionData();
                qrCode = txData.getQrCode();
                qrCodeBase64 = txData.getQrCodeBase64();
                ticketUrl = txData.getTicketUrl();
            }

            return PixPaymentResponse.builder()
                    .mpPaymentId(mpPayment.getId())
                    .status(mpPayment.getStatus())
                    .qrCode(qrCode)
                    .qrCodeBase64(qrCodeBase64)
                    .ticketUrl(ticketUrl)
                    .dateOfExpiration(mpPayment.getDateOfExpiration())
                    .build();

        } catch (Exception e) {
            log.error("Erro ao criar pagamento PIX no Mercado Pago Sandbox", e);
            throw new GatewayUnavailableException(
                    "Falha ao criar pagamento PIX no Mercado Pago: " + e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────
    //  Cartão (domain.payment.gateway.PaymentGatewayPort)
    // ──────────────────────────────────────────────

    @Override
    public PaymentGatewayResponse processCardPayment(PaymentGatewayRequest request) {
        assertReady();

        log.info("Processando pagamento via Mercado Pago Sandbox: amount={}, method={}, installments={}",
                request.getTransactionAmount(), request.getPaymentMethodId(), request.getInstallments());

        try {
            PaymentClient client = new PaymentClient();

            PaymentCreateRequest mpRequest = PaymentCreateRequest.builder()
                    .transactionAmount(request.getTransactionAmount())
                    .token(request.getCardToken())
                    .description(request.getDescription())
                    .installments(request.getInstallments())
                    .paymentMethodId(request.getPaymentMethodId())
                    .issuerId(request.getIssuerId())
                    .payer(buildCardPayer(request))
                    .build();

            MPRequestOptions requestOptions = MPRequestOptions.builder()
                    .customHeaders(Map.of("x-idempotency-key", request.getIdempotencyKey()))
                    .build();

            com.mercadopago.resources.payment.Payment mpPayment = client.create(mpRequest, requestOptions);

            log.info("Resposta Mercado Pago Sandbox Cartão: id={}, status={}, statusDetail={}",
                    mpPayment.getId(), mpPayment.getStatus(), mpPayment.getStatusDetail());

            return PaymentGatewayResponse.builder()
                    .externalId(mpPayment.getId().toString())
                    .externalStatus(mpPayment.getStatus())
                    .externalStatusDetail(mpPayment.getStatusDetail())
                    .paymentTypeId(mpPayment.getPaymentTypeId())
                    .dateApproved(mpPayment.getDateApproved() != null
                            ? mpPayment.getDateApproved().toString() : null)
                    .paymentMethodId(mpPayment.getPaymentMethodId())
                    .installments(mpPayment.getInstallments())
                    .build();

        } catch (Exception e) {
            log.error("Erro ao processar pagamento no Mercado Pago Sandbox", e);
            throw new GatewayUnavailableException(
                    "Falha na comunicação com o gateway de pagamento: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentGatewayResponse refundPayment(String gatewayPaymentId) {
        assertReady();

        log.info("Reembolsando pagamento no Mercado Pago Sandbox: gatewayPaymentId={}", gatewayPaymentId);

        try {
            PaymentClient client = new PaymentClient();
            Long paymentId = Long.valueOf(gatewayPaymentId);

            com.mercadopago.resources.payment.PaymentRefund refund = client.refund(paymentId);

            log.info("Reembolso Mercado Pago Sandbox: refundId={}, paymentId={}, status={}",
                    refund.getId(), refund.getPaymentId(), refund.getStatus());

            return PaymentGatewayResponse.builder()
                    .externalId(refund.getPaymentId().toString())
                    .externalStatus(refund.getStatus())
                    .build();

        } catch (Exception e) {
            log.error("Erro ao reembolsar pagamento no Mercado Pago Sandbox: paymentId={}", gatewayPaymentId, e);
            throw new GatewayUnavailableException(
                    "Falha ao reembolsar no gateway de pagamento: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentGatewayResponse getPayment(String gatewayPaymentId) {
        assertReady();

        log.info("Consultando pagamento no Mercado Pago Sandbox: gatewayPaymentId={}", gatewayPaymentId);

        try {
            PaymentClient client = new PaymentClient();
            Long paymentId = Long.valueOf(gatewayPaymentId);

            com.mercadopago.resources.payment.Payment mpPayment = client.get(paymentId);

            log.info("Status Mercado Pago Sandbox: id={}, status={}, statusDetail={}",
                    mpPayment.getId(), mpPayment.getStatus(), mpPayment.getStatusDetail());

            return PaymentGatewayResponse.builder()
                    .externalId(mpPayment.getId().toString())
                    .externalStatus(mpPayment.getStatus())
                    .externalStatusDetail(mpPayment.getStatusDetail())
                    .paymentTypeId(mpPayment.getPaymentTypeId())
                    .dateApproved(mpPayment.getDateApproved() != null
                            ? mpPayment.getDateApproved().toString() : null)
                    .paymentMethodId(mpPayment.getPaymentMethodId())
                    .installments(mpPayment.getInstallments())
                    .build();

        } catch (Exception e) {
            log.error("Erro ao consultar pagamento no Mercado Pago Sandbox: paymentId={}", gatewayPaymentId, e);
            throw new GatewayUnavailableException(
                    "Falha ao consultar pagamento no gateway: " + e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────
    //  Métodos auxiliares
    // ──────────────────────────────────────────────

    private PaymentPayerRequest buildCardPayer(PaymentGatewayRequest request) {
        PaymentPayerRequest.PaymentPayerRequestBuilder builder = PaymentPayerRequest.builder()
                .email(request.getPayerEmail());

        if (request.getIdentificationType() != null && request.getIdentificationNumber() != null) {
            builder.identification(
                    IdentificationRequest.builder()
                            .type(request.getIdentificationType())
                            .number(request.getIdentificationNumber())
                            .build()
            );
        }

        return builder.build();
    }

    /**
     * Verifica se o gateway está pronto para operar.
     * Lança exceção se o token sandbox não foi configurado.
     */
    private void assertReady() {
        if (!ready) {
            throw new GatewayUnavailableException(
                    "Mercado Pago Sandbox não configurado. Configure MERCADOPAGO_ACCESS_TOKEN com um token TEST-.");
        }
    }

    /**
     * Mascara o token para exibição segura em logs (mostra só os primeiros e últimos caracteres).
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 12) {
            return "***";
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}
