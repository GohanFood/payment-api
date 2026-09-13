package com.delivery.payment.adapter.out.gateway;

import com.delivery.payment.application.dto.response.PixPaymentResponse;
import com.delivery.payment.config.MercadoPagoProperties;
import com.delivery.payment.domain.customer.AddCardCommand;
import com.delivery.payment.domain.customer.CreateCustomerCommand;
import com.delivery.payment.domain.customer.CustomerCardReference;
import com.delivery.payment.domain.payment.exception.GatewayUnavailableException;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayRequest;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayResponse;
import com.delivery.payment.port.CustomerGatewayPort;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.customer.CustomerCardCreateRequest;
import com.mercadopago.client.customer.CustomerClient;
import com.mercadopago.client.customer.CustomerRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.MPSearchRequest;
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
 * <p>Use tokens sandbox com prefixo {@code TEST-} (padrão) ou tokens de produção
 * {@code APP_USR-} quando {@code MERCADOPAGO_ENVIRONMENT=production}. Obtenha as
 * credenciais em:
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
                   com.delivery.payment.domain.payment.gateway.PaymentGatewayPort,
                   CustomerGatewayPort {

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

        if (properties.isProductionToken() && !properties.isProductionEnvironment()) {
            log.error("============================================================");
            log.error("  TOKEN DE PRODUÇÃO DETECTADO — ambiente configurado como '{}'.", properties.getEnvironment());
            log.error("  O token '{}' começa com '{}' (produção).",
                    maskToken(properties.getAccessToken()), TOKEN_PREFIX_PRODUCTION);
            log.error("  Use um token sandbox (prefixo {}) ou configure", TOKEN_PREFIX_SANDBOX);
            log.error("  MERCADOPAGO_ENVIRONMENT=production.");
            log.error("============================================================");
            throw new IllegalStateException(
                    "Token de produção detectado com ambiente sandbox. Configure MERCADOPAGO_ENVIRONMENT=production.");
        }

        if (!properties.isSandboxToken() && !properties.isProductionToken()) {
            log.warn("Token Mercado Pago não parece ser de sandbox nem de produção (prefixos '{}' ou '{}').",
                    TOKEN_PREFIX_SANDBOX, TOKEN_PREFIX_PRODUCTION);
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

        if (!request.usesSavedCard()
                && (request.getPaymentMethodId() == null || request.getPaymentMethodId().isBlank())) {
            throw new IllegalArgumentException(
                    "paymentMethodId é obrigatório. Use a bandeira do cartão: visa, master, elo, amex, etc.");
        }

        log.info("Processando pagamento via Mercado Pago: amount={}, method={}, installments={}, savedCard={}",
                request.getTransactionAmount(), request.getPaymentMethodId(), request.getInstallments(),
                request.usesSavedCard());

        try {
            PaymentClient client = new PaymentClient();

            PaymentCreateRequest mpRequest = buildCardPaymentRequest(request);

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
            String mpErrorBody = extractMercadoPagoError(e);
            log.error("Erro ao processar pagamento no Mercado Pago Sandbox: {}", mpErrorBody, e);
            throw new MercadoPagoIntegrationException(
                    "Falha ao processar pagamento no Mercado Pago: " + mpErrorBody, e);
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
    //  Customer + Card (CustomerGatewayPort)
    // ──────────────────────────────────────────────

    @Override
    public CustomerCardReference createCustomerWithCard(CreateCustomerCommand command) {
        assertReady();

        log.info("Obtendo ou criando Customer e salvando cartão no Mercado Pago: email={}", command.getEmail());

        try {
            CustomerClient client = new CustomerClient();

            CustomerRequest customerRequest = CustomerRequest.builder()
                    .email(command.getEmail())
                    .firstName(command.getFirstName())
                    .lastName(command.getLastName())
                    .identification(buildIdentification(command.getDocumentType(), command.getDocumentNumber()))
                    .build();

            com.mercadopago.resources.customer.Customer customer = findCustomerByEmail(client, command.getEmail());
            if (customer == null) {
                customer = client.create(customerRequest);
                log.info("Customer criado no Mercado Pago: customerId={}", customer.getId());
            } else {
                log.info("Customer existente reutilizado no Mercado Pago: customerId={}", customer.getId());
            }

            com.mercadopago.resources.customer.CustomerCard card = client.createCard(
                    customer.getId(),
                    CustomerCardCreateRequest.builder()
                            .token(command.getCardToken())
                            .paymentMethodId(command.getPaymentMethodId())
                            .build());

            return toReference(customer.getId(), card, command.getPaymentMethodId());

        } catch (Exception e) {
            String mpErrorBody = extractMercadoPagoError(e);
            log.error("Erro ao salvar cartão no Mercado Pago: {}", mpErrorBody, e);
            throw new MercadoPagoIntegrationException(
                    "Falha ao salvar cartão no Mercado Pago: " + mpErrorBody, e);
        }
    }

    @Override
    public CustomerCardReference addCard(String customerId, AddCardCommand command) {
        assertReady();

        log.info("Adicionando/trocando cartão no Mercado Pago: customerId={}", customerId);

        try {
            CustomerClient client = new CustomerClient();

            com.mercadopago.resources.customer.CustomerCard card = client.createCard(
                    customerId,
                    CustomerCardCreateRequest.builder()
                            .token(command.getCardToken())
                            .paymentMethodId(command.getPaymentMethodId())
                            .build());

            return toReference(customerId, card, command.getPaymentMethodId());

        } catch (Exception e) {
            String mpErrorBody = extractMercadoPagoError(e);
            log.error("Erro ao adicionar cartão no Mercado Pago: {}", mpErrorBody, e);
            throw new MercadoPagoIntegrationException(
                    "Falha ao adicionar cartão no Mercado Pago: " + mpErrorBody, e);
        }
    }

    @Override
    public void deleteCard(String customerId, String cardId) {
        assertReady();

        log.info("Removendo cartão no Mercado Pago: customerId={}, cardId={}", customerId, cardId);

        try {
            CustomerClient client = new CustomerClient();
            client.deleteCard(customerId, cardId);
        } catch (Exception e) {
            String mpErrorBody = extractMercadoPagoError(e);
            log.error("Erro ao remover cartão no Mercado Pago: {}", mpErrorBody, e);
            throw new MercadoPagoIntegrationException(
                    "Falha ao remover cartão no Mercado Pago: " + mpErrorBody, e);
        }
    }

    // ──────────────────────────────────────────────
    //  Métodos auxiliares
    // ──────────────────────────────────────────────

    /**
     * O Mercado Pago não permite mais de um Customer para o mesmo e-mail.
     * A busca torna o cadastro idempotente e permite anexar um novo cartão
     * quando o comprador volta a assinar ou troca o meio de pagamento.
     */
    private com.mercadopago.resources.customer.Customer findCustomerByEmail(
            CustomerClient client, String email) throws MPException, MPApiException {
        var page = client.search(MPSearchRequest.builder()
                .filters(Map.of("email", email))
                .limit(1)
                // sdk-java 2.8.0 inclui offset na query; não pode ser nulo.
                .offset(0)
                .build());

        return page.getResults().isEmpty() ? null : page.getResults().get(0);
    }

    private PaymentCreateRequest buildCardPaymentRequest(PaymentGatewayRequest request) {
        if (request.usesSavedCard()) {
            return PaymentCreateRequest.builder()
                    .transactionAmount(request.getTransactionAmount())
                    .token(request.getCardId())
                    .description(request.getDescription())
                    .installments(request.getInstallments())
                    .paymentMethodId(request.getPaymentMethodId())
                    .payer(buildSavedCardPayer(request))
                    .build();
        }

        return PaymentCreateRequest.builder()
                .transactionAmount(request.getTransactionAmount())
                .token(request.getCardToken())
                .description(request.getDescription())
                .installments(request.getInstallments())
                .paymentMethodId(request.getPaymentMethodId())
                .issuerId(request.getIssuerId())
                .payer(buildCardPayer(request))
                .build();
    }

    private PaymentPayerRequest buildSavedCardPayer(PaymentGatewayRequest request) {
        PaymentPayerRequest.PaymentPayerRequestBuilder builder = PaymentPayerRequest.builder()
                .type("customer")
                .id(request.getCustomerId());

        if (request.getPayerEmail() != null && !request.getPayerEmail().isBlank()) {
            builder.email(request.getPayerEmail());
        }

        return builder.build();
    }

    private PaymentPayerRequest buildCardPayer(PaymentGatewayRequest request) {
        PaymentPayerRequest.PaymentPayerRequestBuilder builder = PaymentPayerRequest.builder()
                .email(request.getPayerEmail())
                .entityType("individual");

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

    private IdentificationRequest buildIdentification(String documentType, String documentNumber) {
        if (documentType == null || documentType.isBlank()
                || documentNumber == null || documentNumber.isBlank()) {
            return null;
        }
        return IdentificationRequest.builder()
                .type(documentType)
                .number(documentNumber)
                .build();
    }

    private CustomerCardReference toReference(
            String customerId,
            com.mercadopago.resources.customer.CustomerCard card,
            String fallbackPaymentMethodId) {
        String paymentMethodId = card.getPaymentMethod() != null
                ? card.getPaymentMethod().getId()
                : fallbackPaymentMethodId;
        return CustomerCardReference.builder()
                .customerId(customerId)
                .cardId(card.getId())
                .paymentMethodId(paymentMethodId)
                .build();
    }

    /**
     * Extrai a mensagem de erro detalhada das exceções do Mercado Pago.
     */
    private String extractMercadoPagoError(Exception e) {
        if (e instanceof MPApiException apiEx) {
            try {
                return apiEx.getApiResponse() != null
                        ? apiEx.getApiResponse().getContent()
                        : apiEx.getMessage();
            } catch (Exception ex) {
                return apiEx.getMessage();
            }
        }
        if (e instanceof MPException mpEx) {
            return mpEx.getMessage();
        }
        return e.getMessage() != null ? e.getMessage() : "Erro desconhecido";
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
