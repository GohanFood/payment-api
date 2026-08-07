package com.delivery.payment.port;

import com.delivery.payment.domain.payment.Payment;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    List<Payment> findByUserId(UUID userId);

    List<Payment> findByOrderId(UUID orderId);

    void delete(Payment payment);
}
