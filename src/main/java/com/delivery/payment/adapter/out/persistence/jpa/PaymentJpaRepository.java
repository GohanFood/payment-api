package com.delivery.payment.adapter.out.persistence.jpa;

import com.delivery.payment.adapter.out.persistence.entity.PaymentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    List<PaymentEntity> findByUserId(String userId);

    List<PaymentEntity> findByUserId(String userId, Pageable pageable);

    long countByUserId(String userId);

    List<PaymentEntity> findByOrderId(UUID orderId);

    Optional<PaymentEntity> findByMpPaymentId(Long mpPaymentId);

    Optional<PaymentEntity> findByGatewayTransactionId(String gatewayTransactionId);
}
