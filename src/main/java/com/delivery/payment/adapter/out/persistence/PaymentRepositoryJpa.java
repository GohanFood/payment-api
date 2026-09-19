package com.delivery.payment.adapter.out.persistence;

import com.delivery.payment.adapter.out.persistence.jpa.PaymentJpaRepository;
import com.delivery.payment.adapter.out.persistence.mapper.PaymentEntityMapper;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    public Payment saveAndFlush(Payment payment) {
        var entity = mapper.toEntity(payment);
        var saved = jpaRepository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Payment> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Payment> findByUserId(String userId, int page, int size) {
        return jpaRepository.findByUserId(userId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByUserId(String userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    public List<Payment> findByReferenceId(String referenceId) {
        return jpaRepository.findByReferenceId(referenceId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Payment> findByMpPaymentId(Long mpPaymentId) {
        return jpaRepository.findByMpPaymentId(mpPaymentId).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId) {
        return jpaRepository.findByGatewayTransactionId(gatewayTransactionId).map(mapper::toDomain);
    }

    @Override
    public List<Payment> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void delete(Payment payment) {
        jpaRepository.deleteById(payment.getId());
    }
}
