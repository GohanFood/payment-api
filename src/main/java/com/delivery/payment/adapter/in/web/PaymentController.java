package com.delivery.payment.adapter.in.web;

import com.delivery.payment.application.dto.request.CreatePaymentRequest;
import com.delivery.payment.application.dto.request.ProcessPaymentRequest;
import com.delivery.payment.application.dto.request.RefundPaymentRequest;
import com.delivery.payment.application.dto.response.PaymentResponse;
import com.delivery.payment.application.usecase.*;
import com.delivery.payment.domain.payment.Payment;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;
    private final ProcessPaymentUseCase processPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final ListPaymentsUseCase listPaymentsUseCase;
    private final RefundPaymentUseCase refundPaymentUseCase;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = Payment.builder()
                .userId(request.getUserId())
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .build();

        Payment created = createPaymentUseCase.execute(payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> process(@PathVariable UUID id,
                                                    @Valid @RequestBody ProcessPaymentRequest request) {
        Payment processed = processPaymentUseCase.execute(id, request.getGatewayToken());
        return ResponseEntity.ok(toResponse(processed));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> getById(@PathVariable UUID id) {
        Payment payment = getPaymentUseCase.execute(id);
        return ResponseEntity.ok(toResponse(payment));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PaymentResponse>> listByUser(@RequestParam UUID userId) {
        List<Payment> payments = listPaymentsUseCase.execute(userId);
        return ResponseEntity.ok(payments.stream().map(this::toResponse).toList());
    }

    @PostMapping("/refund")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> refund(@Valid @RequestBody RefundPaymentRequest request) {
        Payment refunded = refundPaymentUseCase.execute(request.getPaymentId());
        return ResponseEntity.ok(toResponse(refunded));
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .userId(payment.getUserId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
