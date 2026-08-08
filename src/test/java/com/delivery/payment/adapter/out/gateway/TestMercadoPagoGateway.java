package com.delivery.payment.adapter.out.gateway;

import com.delivery.payment.application.dto.response.PixPaymentResponse;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayRequest;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stub do MercadoPagoGateway exclusivo para o perfil de teste.
 *
 * <p>Substitui o gateway real (que requer token sandbox) por respostas
 * mockadas, permitindo que os testes de integração rodem sem credenciais.</p>
 */
@Slf4j
@Component
@Primary
@Profile("test")
public class TestMercadoPagoGateway
        implements com.delivery.payment.port.PaymentGatewayPort,
                   com.delivery.payment.domain.payment.gateway.PaymentGatewayPort {

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

        log.info("Test MP Stub: criando PIX amount={}", amount);
        return PixPaymentResponse.builder()
                .mpPaymentId(-1L)
                .status("pending")
                .qrCode("00020126580014br.gov.bcb.pix0136test@mercadopago.com.br")
                .qrCodeBase64("test-base64")
                .ticketUrl("https://www.mercadopago.com.br/payments/test/ticket")
                .build();
    }

    @Override
    public PaymentGatewayResponse processCardPayment(PaymentGatewayRequest request) {
        log.info("Test MP Stub: processando cartão amount={}", request.getTransactionAmount());
        return PaymentGatewayResponse.builder()
                .externalId("TEST-" + System.currentTimeMillis())
                .externalStatus("approved")
                .externalStatusDetail("accredited")
                .paymentTypeId("credit_card")
                .build();
    }

    @Override
    public PaymentGatewayResponse refundPayment(String gatewayPaymentId) {
        log.info("Test MP Stub: reembolsando paymentId={}", gatewayPaymentId);
        return PaymentGatewayResponse.builder()
                .externalId(gatewayPaymentId)
                .externalStatus("refunded")
                .build();
    }

    @Override
    public PaymentGatewayResponse getPayment(String gatewayPaymentId) {
        log.info("Test MP Stub: consultando paymentId={}", gatewayPaymentId);
        return PaymentGatewayResponse.builder()
                .externalId(gatewayPaymentId)
                .externalStatus("approved")
                .externalStatusDetail("accredited")
                .build();
    }
}
