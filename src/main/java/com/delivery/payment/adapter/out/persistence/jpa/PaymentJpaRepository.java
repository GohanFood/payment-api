package com.delivery.payment.adapter.out.persistence.jpa;

import com.delivery.payment.adapter.out.persistence.entity.PaymentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    List<PaymentEntity> findByUserId(UUID userId);

    List<PaymentEntity> findByUserId(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);

    List<PaymentEntity> findByOrderId(UUID orderId);

    Optional<PaymentEntity> findByMpPaymentId(Long mpPaymentId);

    Optional<PaymentEntity> findByGatewayTransactionId(String gatewayTransactionId);
}
