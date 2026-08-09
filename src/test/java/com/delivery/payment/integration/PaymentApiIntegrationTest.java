package com.delivery.payment.integration;

import com.delivery.payment.adapter.out.messaging.KafkaPaymentConsumer;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
import com.delivery.payment.port.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private KafkaPaymentConsumer kafkaPaymentConsumer;

    private String jwtToken;

    private static final String JWT_SECRET = "YnVzY2FuZG9Vc2VyQVBJU2VjcmV0QXNzZW1ibHlBcHBsaWNhdGlvbjIwMjY=";

    @BeforeEach
    void setUp() {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        jwtToken = Jwts.builder()
                .subject("user-integration-test")
                .claim("role", "USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    @Test
    void shouldCreateAndRetrievePayment() throws Exception {
        UUID orderId = UUID.randomUUID();

        Map<String, Object> request = Map.of(
                "orderId", orderId.toString(),
                "amount", 150.00,
                "paymentMethod", "CREDIT_CARD"
        );

        String response = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
                .andReturn().getResponse().getContentAsString();

        // Verify that the payment was persisted
        String id = objectMapper.readTree(response).get("id").asText();
        UUID paymentId = UUID.fromString(id);

        Optional<Payment> saved = paymentRepository.findById(paymentId);
        assertThat(saved).isPresent();
        assertThat(saved.get().getStatus()).isEqualTo(PaymentStatus.PENDING);

        // Retrieve by ID
        mockMvc.perform(get("/api/v1/payments/" + paymentId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldCreateAndProcessPayment() throws Exception {
        UUID orderId = UUID.randomUUID();

        Map<String, Object> createRequest = Map.of(
                "orderId", orderId.toString(),
                "amount", 200.00,
                "paymentMethod", "PIX"
        );

        String createResponse = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String paymentId = objectMapper.readTree(createResponse).get("id").asText();

        // Process payment
        Map<String, Object> processRequest = Map.of(
                "gatewayToken", "tok-test-123",
                "payerEmail", "test@email.com"
        );

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/process")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(processRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.gatewayTransactionId").exists());

        // Verify re-processing returns 409
        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/process")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(processRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldListPaymentsByUser() throws Exception {
        // Create 2 payments for the same user (userId from JWT)
        for (int i = 0; i < 2; i++) {
            Map<String, Object> request = Map.of(
                    "orderId", UUID.randomUUID().toString(),
                    "amount", 100.00 + i,
                    "paymentMethod", "CREDIT_CARD"
            );
            mockMvc.perform(post("/api/v1/payments")
                    .header("Authorization", "Bearer " + jwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/payments")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldRefundPayment() throws Exception {
        UUID orderId = UUID.randomUUID();

        // Create payment
        Map<String, Object> createRequest = Map.of(
                "orderId", orderId.toString(),
                "amount", 300.00,
                "paymentMethod", "PIX"
        );

        String createResponse = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String paymentId = objectMapper.readTree(createResponse).get("id").asText();

        // Process payment to COMPLETED before refunding
        Map<String, Object> processRequest = Map.of(
                "gatewayToken", "tok-test-refund",
                "payerEmail", "test@email.com"
        );

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/process")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(processRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Refund
        Map<String, Object> refundRequest = Map.of("paymentId", paymentId);

        mockMvc.perform(post("/api/v1/payments/refund")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        // Verify re-refunding returns 409
        mockMvc.perform(post("/api/v1/payments/refund")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn404ForNonExistentPayment() throws Exception {
        UUID fakeId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/payments/" + fakeId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenNoAuthToken() throws Exception {
        mockMvc.perform(get("/api/v1/payments/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectInvalidAmount() throws Exception {
        Map<String, Object> request = Map.of(
                "orderId", UUID.randomUUID().toString(),
                "amount", 0,
                "paymentMethod", "PIX"
        );

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNegativeAmount() throws Exception {
        Map<String, Object> request = Map.of(
                "orderId", UUID.randomUUID().toString(),
                "amount", -50.00,
                "paymentMethod", "PIX"
        );

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
