package com.delivery.payment.application.service;

import com.delivery.payment.application.dto.request.ProcessPaymentRequest;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
import com.delivery.payment.domain.payment.exception.PaymentAlreadyProcessedException;
import com.delivery.payment.domain.payment.exception.PaymentNotFoundException;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayPort;
import com.delivery.payment.domain.payment.gateway.PaymentGatewayResponse;
import com.delivery.payment.port.PaymentMessagingPort;
import com.delivery.payment.port.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMessagingPort paymentMessagingPort;

    @Mock
    private PaymentGatewayPort paymentGateway;

    private ProcessPaymentService service;

    private ProcessPaymentRequest validRequest;

    @BeforeEach
    void setUp() {
        service = new ProcessPaymentService(paymentRepository, paymentMessagingPort, paymentGateway);
        validRequest = ProcessPaymentRequest.builder()
                .gatewayToken("token-xyz")
                .payerEmail("test@email.com")
                .installments(1)
                .paymentMethodId("visa")
                .build();
    }

    @Test
    void shouldProcessPaymentSuccessfully() {
        UUID paymentId = UUID.randomUUID();
        String referenceId = "subscription:" + UUID.randomUUID();
        Payment pending = Payment.builder()
                .id(paymentId)
                .userId("user-1")
                .referenceId(referenceId)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PaymentGatewayResponse gatewayResponse = PaymentGatewayResponse.builder()
                .externalId("123456")
                .externalStatus("approved")
                .externalStatusDetail("accredited")
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(pending));
        when(paymentGateway.processCardPayment(any())).thenReturn(gatewayResponse);
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.execute(paymentId, validRequest);

        assertNotNull(result);
        assertEquals(PaymentStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getGatewayTransactionId());
        verify(paymentMessagingPort).publishPaymentCompleted(eq(paymentId), eq(referenceId));
    }

    @Test
    void shouldThrowExceptionWhenPaymentNotFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () ->
                service.execute(paymentId, validRequest));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenPaymentAlreadyProcessed() {
        UUID paymentId = UUID.randomUUID();
        Payment completed = Payment.builder()
                .id(paymentId)
                .userId("user-1")
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(completed));

        assertThrows(PaymentAlreadyProcessedException.class, () ->
                service.execute(paymentId, validRequest));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenPaymentFailed() {
        UUID paymentId = UUID.randomUUID();
        Payment failed = Payment.builder()
                .id(paymentId)
                .userId("user-1")
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.FAILED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(failed));

        assertThrows(PaymentAlreadyProcessedException.class, () ->
                service.execute(paymentId, validRequest));
    }

    @Test
    void shouldThrowExceptionWhenPaymentRefunded() {
        UUID paymentId = UUID.randomUUID();
        Payment refunded = Payment.builder()
                .id(paymentId)
                .userId("user-1")
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.REFUNDED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(refunded));

        assertThrows(PaymentAlreadyProcessedException.class, () ->
                service.execute(paymentId, validRequest));
    }
}
