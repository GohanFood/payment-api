package com.delivery.payment.adapter.in.web;

import com.delivery.payment.application.dto.request.CreatePaymentRequest;
import com.delivery.payment.application.dto.request.ProcessPaymentRequest;
import com.delivery.payment.application.dto.request.RefundPaymentRequest;
import com.delivery.payment.application.service.PaymentStatusSyncService;
import com.delivery.payment.application.usecase.*;
import com.delivery.payment.config.JwtConfig;
import com.delivery.payment.config.MercadoPagoWebhookValidator;
import com.delivery.payment.config.SecurityConfig;
import com.delivery.payment.adapter.out.callback.SubscriptionPaymentCallbackClient;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
import com.delivery.payment.domain.payment.exception.PaymentNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, JwtConfig.class, JwtAuthenticationFilter.class})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreatePaymentUseCase createPaymentUseCase;

    @MockBean
    private ProcessPaymentUseCase processPaymentUseCase;

    @MockBean
    private GetPaymentUseCase getPaymentUseCase;

    @MockBean
    private ListPaymentsUseCase listPaymentsUseCase;

    @MockBean
    private RefundPaymentUseCase refundPaymentUseCase;

    @MockBean
    private PaymentStatusSyncService paymentStatusSyncService;

    @MockBean
    private JwtConfig jwtConfig;

    @MockBean
    private MercadoPagoWebhookValidator webhookValidator;

    @MockBean
    private SubscriptionPaymentCallbackClient paymentCallbackClient;

    private String jwtToken;
    private static final String TEST_USER_ID = "user-123";

    @BeforeEach
    void setUp() {
        when(jwtConfig.getSecret()).thenReturn("test-secret-key-for-testing-purposes-only");

        SecretKey key = Keys.hmacShaKeyFor("test-secret-key-for-testing-purposes-only".getBytes(StandardCharsets.UTF_8));
        jwtToken = Jwts.builder()
                .subject(TEST_USER_ID)
                .claim("role", "USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    @Test
    void shouldCreatePayment() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setReferenceId("subscription:" + UUID.randomUUID());
        request.setAmount(new BigDecimal("150.00"));
        request.setPaymentMethod("CREDIT_CARD");

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .userId(TEST_USER_ID)
                .referenceId(request.getReferenceId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(createPaymentUseCase.execute(any(CreatePaymentRequest.class), eq(TEST_USER_ID))).thenReturn(payment);

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldRejectUnauthorizedRequest() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setReferenceId("subscription:" + UUID.randomUUID());
        request.setAmount(new BigDecimal("100.00"));
        request.setPaymentMethod("PIX");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldProcessPayment() throws Exception {
        UUID paymentId = UUID.randomUUID();
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setGatewayToken("token-xyz");
        request.setPayerEmail("test@email.com");

        Payment payment = Payment.builder()
                .id(paymentId)
                .userId(TEST_USER_ID)
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("150.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.COMPLETED)
                .gatewayTransactionId("GW-123")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(processPaymentUseCase.execute(eq(paymentId), any(ProcessPaymentRequest.class))).thenReturn(payment);

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/process")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.gatewayTransactionId").value("GW-123"));
    }

    @Test
    void shouldGetPaymentById() throws Exception {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .userId(TEST_USER_ID)
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("200.00"))
                .paymentMethod("PIX")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(getPaymentUseCase.execute(paymentId)).thenReturn(payment);

        mockMvc.perform(get("/api/v1/payments/" + paymentId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.amount").value(200.00));
    }

    @Test
    void shouldGetPaymentByIdNotFound() throws Exception {
        UUID paymentId = UUID.randomUUID();
        when(getPaymentUseCase.execute(paymentId)).thenThrow(new PaymentNotFoundException(paymentId));

        mockMvc.perform(get("/api/v1/payments/" + paymentId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListPaymentsByUser() throws Exception {
        Payment p1 = Payment.builder()
                .id(UUID.randomUUID())
                .userId(TEST_USER_ID)
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(listPaymentsUseCase.execute(eq(TEST_USER_ID), eq(0), eq(20))).thenReturn(List.of(p1));

        mockMvc.perform(get("/api/v1/payments")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].amount").value(100.00));
    }

    @Test
    void shouldRefundPayment() throws Exception {
        UUID paymentId = UUID.randomUUID();
        RefundPaymentRequest request = new RefundPaymentRequest();
        request.setPaymentId(paymentId);

        Payment payment = Payment.builder()
                .id(paymentId)
                .userId(TEST_USER_ID)
                .referenceId("subscription:" + UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .status(PaymentStatus.REFUNDED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(refundPaymentUseCase.execute(paymentId)).thenReturn(payment);

        mockMvc.perform(post("/api/v1/payments/refund")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }
}
