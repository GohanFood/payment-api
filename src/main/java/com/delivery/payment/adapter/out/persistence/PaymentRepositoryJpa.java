package com.delivery.payment.adapter.out.persistence;

import com.delivery.payment.adapter.out.persistence.jpa.PaymentJpaRepository;
import com.delivery.payment.adapter.out.persistence.mapper.PaymentEntityMapper;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryJpa implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;
    private final PaymentEntityMapper mapper;

    @Override
    public Payment save(Payment payment) {
        var entity = mapper.toEntity(payment);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Payment> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Payment> findByOrderId(UUID orderId) {
        return jpaRepository.findByOrderId(orderId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void delete(Payment payment) {
        jpaRepository.deleteById(payment.getId());
    }
}
