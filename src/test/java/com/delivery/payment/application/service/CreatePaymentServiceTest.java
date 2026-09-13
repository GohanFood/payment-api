package com.delivery.payment.application.service;

import com.delivery.payment.application.dto.request.CreatePaymentRequest;
import com.delivery.payment.application.dto.response.PixPaymentResponse;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
import com.delivery.payment.domain.payment.exception.InvalidPaymentAmountException;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayPort;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayRequest;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayResponse;
import com.delivery.payment.port.PaymentMessagingPort;
import com.delivery.payment.port.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMessagingPort paymentMessagingPort;

    @Mock
    private com.delivery.payment.port.PaymentGatewayPort pixGateway;

    @Mock
    private PaymentGatewayPort cardGateway;

    private CreatePaymentService service;

    private static final String USER_ID = "user-1";

    @BeforeEach
    void setUp() {
        service = new CreatePaymentService(paymentRepository, paymentMessagingPort, pixGateway, cardGateway);
    }

    private CreatePaymentRequest buildRequest(String method) {
        return CreatePaymentRequest.builder()
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("150.00"))
                .paymentMethod(method)
                .payerEmail("test@email.com")
                .build();
    }

    @Test
    void shouldCreatePaymentSuccessfully() {
        CreatePaymentRequest request = buildRequest("CREDIT_CARD");

        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.execute(request, USER_ID);

        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals(request.getAmount(), result.getAmount());
        assertEquals(USER_ID, result.getUserId());
        verify(paymentRepository).save(any());
        verify(paymentMessagingPort).publishPaymentCreated(any(), any());
    }

    @Test
    void shouldRejectZeroAmount() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(BigDecimal.ZERO)
                .paymentMethod("PIX")
                .build();

        assertThrows(InvalidPaymentAmountException.class, () -> service.execute(request, USER_ID));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldRejectNegativeAmount() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("-10.00"))
                .paymentMethod("PIX")
                .build();

        assertThrows(InvalidPaymentAmountException.class, () -> service.execute(request, USER_ID));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldCreatePixPaymentAndCallMercadoPago() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("200.00"))
                .paymentMethod("PIX")
                .payerEmail("pix@email.com")
                .payerDocumentType("CPF")
                .payerDocumentNumber("12345678901")
                .build();

        PixPaymentResponse mpResponse = PixPaymentResponse.builder()
                .mpPaymentId(123L)
                .status("pending")
                .qrCode("test-qr-code")
                .qrCodeBase64("test-base64")
                .ticketUrl("https://mp.com/ticket")
                .build();

        when(pixGateway.createPixPayment(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mpResponse);
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.execute(request, USER_ID);

        assertNotNull(result.getMpPaymentId());
        assertEquals(123L, result.getMpPaymentId());
        assertEquals("test-qr-code", result.getQrCode());
        verify(pixGateway).createPixPayment(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldCreateCardPaymentAndCallMercadoPago() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("89.90"))
                .paymentMethod("CREDIT_CARD")
                .payerEmail("card@email.com")
                .payerDocumentType("CPF")
                .payerDocumentNumber("12345678901")
                .gatewayToken("card-token-xyz")
                .paymentMethodId("master")
                .installments(1)
                .description("Teste cartão")
                .build();

        PaymentGatewayResponse gatewayResponse = PaymentGatewayResponse.builder()
                .externalId("987654321")
                .externalStatus("approved")
                .externalStatusDetail("accredited")
                .paymentTypeId("credit_card")
                .paymentMethodId("master")
                .installments(1)
                .build();

        when(cardGateway.processCardPayment(any(PaymentGatewayRequest.class))).thenReturn(gatewayResponse);
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.execute(request, USER_ID);

        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertEquals("987654321", result.getGatewayTransactionId());
        verify(cardGateway).processCardPayment(any(PaymentGatewayRequest.class));
    }

    @Test
    void shouldCreateCardPaymentWithSavedCard() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("89.90"))
                .paymentMethod("CREDIT_CARD")
                .cardId("saved-card-id")
                .customerId("customer-id")
                .installments(1)
                .description("Cobrança recorrente")
                .build();

        PaymentGatewayResponse gatewayResponse = PaymentGatewayResponse.builder()
                .externalId("987654321")
                .externalStatus("approved")
                .externalStatusDetail("accredited")
                .paymentTypeId("credit_card")
                .build();

        when(cardGateway.processCardPayment(any(PaymentGatewayRequest.class))).thenReturn(gatewayResponse);
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.execute(request, USER_ID);

        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertEquals("customer-id", result.getCustomerId());
        assertEquals("saved-card-id", result.getCardId());
        verify(cardGateway).processCardPayment(any(PaymentGatewayRequest.class));
    }

    @Test
    void shouldCreateCardPaymentWithoutTokenAsPendingOnly() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("50.00"))
                .paymentMethod("CREDIT_CARD")
                .build();

        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.execute(request, USER_ID);

        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertNull(result.getGatewayTransactionId());
        verify(cardGateway, never()).processCardPayment(any());
    }

    @Test
    void shouldGenerateIdempotencyKey() {
        CreatePaymentRequest request = buildRequest("CREDIT_CARD");

        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.execute(request, USER_ID);

        assertNotNull(result.getId());
        verify(paymentRepository).save(any());
    }
}
