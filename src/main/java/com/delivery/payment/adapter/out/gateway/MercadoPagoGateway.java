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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Adapter que implementa a integração com a API de pagamentos do Mercado Pago
 * para PIX (QR Code) e Cartão de Crédito/Débito (CardForm + CardToken).
 *
 * <h3>Fluxo Cartão:</h3>
 * <ol>
 *   <li>Frontend captura dados do cartão via MercadoPago.js CardForm → gera CardToken</li>
 *   <li>Frontend envia CardToken + dados do comprador para nosso backend</li>
 *   <li>Backend chama processCardPayment() → Mercado Pago API POST /v1/payments</li>
 *   <li>Mercado Pago retorna status: approved, rejected, in_process, pending</li>
 * </ol>
 *
 * <h3>Fluxo PIX:</h3>
 * <ol>
 *   <li>Backend chama createPixPayment() → Mercado Pago API POST /v1/payments</li>
 *   <li>Mercado Pago retorna QR Code (base64 + texto) e ticket_url</li>
 *   <li>Frontend exibe QR Code para o comprador pagar</li>
 * </ol>
 *
 * @see <a href="https://www.mercadopago.com.br/developers/pt/docs/checkout-api-payments/integration-configuration/card/integrate-via-cardform">Cartão via CardForm</a>
 * @see <a href="https://www.mercadopago.com.br/developers/pt/reference/payments/_payments/post">API Reference</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MercadoPagoGateway
        implements com.delivery.payment.port.PaymentGatewayPort,
                   com.delivery.payment.domain.payment.gateway.PaymentGatewayPort {

    private final MercadoPagoProperties properties;
    private boolean simulationMode = false;

    @PostConstruct
    public void init() {
        if (properties.getAccessToken() != null && !properties.getAccessToken().isBlank()) {
            MercadoPagoConfig.setAccessToken(properties.getAccessToken());
            log.info("Mercado Pago SDK inicializado com access token configurado");
        } else {
            simulationMode = true;
            log.warn("Mercado Pago access token não configurado! Usando modo simulado.");
        }
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

        if (simulationMode) {
            log.info("Mercado Pago em modo simulado: criando PIX amount={}", amount);
            return PixPaymentResponse.builder()
                    .mpPaymentId(-1L)
                    .status("pending")
                    .qrCode("00020126580014br.gov.bcb.pix0136simulado@mercadopago.com.br")
                    .qrCodeBase64("simulado-base64")
                    .ticketUrl("https://www.mercadopago.com.br/payments/simulado/ticket")
                    .build();
        }

        log.info("Criando pagamento PIX no Mercado Pago: amount={}, email={}", amount, payerEmail);

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

            log.info("Pagamento PIX criado no Mercado Pago: mpPaymentId={}, status={}",
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
            log.error("Erro ao criar pagamento PIX no Mercado Pago", e);
            throw new GatewayUnavailableException(
                    "Falha ao criar pagamento PIX no Mercado Pago: " + e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────
    //  Cartão (domain.payment.gateway.PaymentGatewayPort)
    // ──────────────────────────────────────────────

    @Override
    public PaymentGatewayResponse processCardPayment(PaymentGatewayRequest request) {
        if (simulationMode) {
            log.info("Mercado Pago em modo simulado: processando cartão amount={}", request.getTransactionAmount());
            return PaymentGatewayResponse.builder()
                    .externalId("SIM-" + System.currentTimeMillis())
                    .externalStatus("approved")
                    .externalStatusDetail("accredited")
                    .paymentTypeId("credit_card")
                    .build();
        }

        log.info("Processando pagamento via Mercado Pago: amount={}, method={}, installments={}",
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

            log.info("Resposta Mercado Pago Cartão: id={}, status={}, statusDetail={}",
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
            log.error("Erro ao processar pagamento no Mercado Pago", e);
            throw new GatewayUnavailableException(
                    "Falha na comunicação com o gateway de pagamento: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentGatewayResponse refundPayment(String gatewayPaymentId) {
        if (simulationMode) {
            log.info("Mercado Pago em modo simulado: reembolsando paymentId={}", gatewayPaymentId);
            return PaymentGatewayResponse.builder()
                    .externalId(gatewayPaymentId)
                    .externalStatus("refunded")
                    .build();
        }

        log.info("Reembolsando pagamento no Mercado Pago: gatewayPaymentId={}", gatewayPaymentId);

        try {
            PaymentClient client = new PaymentClient();
            Long paymentId = Long.valueOf(gatewayPaymentId);

            com.mercadopago.resources.payment.PaymentRefund refund = client.refund(paymentId);

            log.info("Reembolso Mercado Pago: refundId={}, paymentId={}, status={}",
                    refund.getId(), refund.getPaymentId(), refund.getStatus());

            return PaymentGatewayResponse.builder()
                    .externalId(refund.getPaymentId().toString())
                    .externalStatus(refund.getStatus())
                    .build();

        } catch (Exception e) {
            log.error("Erro ao reembolsar pagamento no Mercado Pago: paymentId={}", gatewayPaymentId, e);
            throw new GatewayUnavailableException(
                    "Falha ao reembolsar no gateway de pagamento: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentGatewayResponse getPayment(String gatewayPaymentId) {
        if (simulationMode) {
            log.info("Mercado Pago em modo simulado: consultando paymentId={}", gatewayPaymentId);
            return PaymentGatewayResponse.builder()
                    .externalId(gatewayPaymentId)
                    .externalStatus("approved")
                    .externalStatusDetail("accredited")
                    .build();
        }

        log.info("Consultando pagamento no Mercado Pago: gatewayPaymentId={}", gatewayPaymentId);

        try {
            PaymentClient client = new PaymentClient();
            Long paymentId = Long.valueOf(gatewayPaymentId);

            com.mercadopago.resources.payment.Payment mpPayment = client.get(paymentId);

            log.info("Status Mercado Pago: id={}, status={}, statusDetail={}",
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
            log.error("Erro ao consultar pagamento no Mercado Pago: paymentId={}", gatewayPaymentId, e);
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
}
