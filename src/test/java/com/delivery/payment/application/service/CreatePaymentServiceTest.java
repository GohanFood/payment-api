package com.delivery.payment.application.service;

import com.delivery.payment.application.dto.response.PixPaymentResponse;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
import com.delivery.payment.domain.payment.exception.InvalidPaymentAmountException;
import com.delivery.payment.port.PaymentGatewayPort;
import com.delivery.payment.port.PaymentMessagingPort;
import com.delivery.payment.port.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private PaymentGatewayPort paymentGatewayPort;

    private CreatePaymentService service;

    @BeforeEach
    void setUp() {
        service = new CreatePaymentService(paymentRepository, paymentMessagingPort, paymentGatewayPort);
    }

    @Test
    void shouldCreatePaymentSuccessfully() {
        Payment input = Payment.builder()
                .userId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .amount(new BigDecimal("150.00"))
                .paymentMethod("CREDIT_CARD")
                .build();

        Payment saved = Payment.builder()
                .id(UUID.randomUUID())
                .userId(input.getUserId())
                .orderId(input.getOrderId())
                .amount(input.getAmount())
                .paymentMethod(input.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.save(any())).thenReturn(saved);

        Payment result = service.execute(input);

        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals(input.getAmount(), result.getAmount());
        verify(paymentRepository).save(any());
        verify(paymentMessagingPort).publishPaymentCreated(any(), any());
    }

    @Test
    void shouldRejectZeroAmount() {
        Payment input = Payment.builder()
                .userId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .amount(BigDecimal.ZERO)
                .paymentMethod("PIX")
                .build();

        assertThrows(InvalidPaymentAmountException.class, () -> service.execute(input));
        verify(paymentRepository, never()).save(any());
        verify(paymentMessagingPort, never()).publishPaymentCreated(any(), any());
    }

    @Test
    void shouldRejectNegativeAmount() {
        Payment input = Payment.builder()
                .userId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .amount(new BigDecimal("-10.00"))
                .paymentMethod("PIX")
                .build();

        assertThrows(InvalidPaymentAmountException.class, () -> service.execute(input));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldCreatePaymentWithCorrectFields() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("200.00");

        Payment input = Payment.builder()
                .userId(userId)
                .orderId(orderId)
                .amount(amount)
                .paymentMethod("DEBIT_CARD")
                .build();

        Payment saved = Payment.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .orderId(orderId)
                .amount(amount)
                .paymentMethod("DEBIT_CARD")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.save(any())).thenReturn(saved);

        Payment result = service.execute(input);

        assertEquals(userId, result.getUserId());
        assertEquals(orderId, result.getOrderId());
        assertEquals("DEBIT_CARD", result.getPaymentMethod());
        verify(paymentMessagingPort).publishPaymentCreated(result.getId(), orderId);
    }

    @Test
    void shouldGenerateNewIdForPayment() {
        Payment input = Payment.builder()
                .userId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .amount(new BigDecimal("50.00"))
                .paymentMethod("PIX")
                .build();

        Payment saved = Payment.builder()
                .id(UUID.randomUUID())
                .userId(input.getUserId())
                .orderId(input.getOrderId())
                .amount(input.getAmount())
                .paymentMethod(input.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PixPaymentResponse mpResponse = PixPaymentResponse.builder()
                .mpPaymentId(123L)
                .status("pending")
                .qrCode("test-qr-code")
                .qrCodeBase64("test-base64")
                .ticketUrl("https://mp.com/ticket")
                .build();

        when(paymentGatewayPort.createPixPayment(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mpResponse);
        when(paymentRepository.save(any())).thenReturn(saved);

        Payment result = service.execute(input);

        assertNotNull(result.getId());
        verify(paymentRepository).save(any());
    }
}
