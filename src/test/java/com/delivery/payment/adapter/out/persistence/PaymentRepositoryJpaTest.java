package com.delivery.payment.adapter.out.persistence;

import com.delivery.payment.adapter.out.persistence.entity.PaymentEntity;
import com.delivery.payment.adapter.out.persistence.jpa.PaymentJpaRepository;
import com.delivery.payment.adapter.out.persistence.mapper.PaymentEntityMapper;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRepositoryJpaTest {

    @Mock
    private PaymentJpaRepository jpaRepository;

    private PaymentRepositoryJpa repository;
    private final PaymentEntityMapper mapper = new PaymentEntityMapper();

    @BeforeEach
    void setUp() {
        repository = new PaymentRepositoryJpa(jpaRepository, mapper);
    }

    @Test
    void shouldSavePayment() {
        Payment domain = createPayment(UUID.randomUUID(), "user-1", "CREDIT_CARD", PaymentStatus.PENDING);
        PaymentEntity entity = mapper.toEntity(domain);

        when(jpaRepository.save(any())).thenReturn(entity);

        Payment saved = repository.save(domain);

        assertNotNull(saved);
        assertEquals(domain.getId(), saved.getId());
        assertEquals(domain.getAmount(), saved.getAmount());
        assertEquals(PaymentStatus.PENDING, saved.getStatus());
    }

    @Test
    void shouldFindById() {
        UUID paymentId = UUID.randomUUID();
        PaymentEntity entity = createEntity(paymentId, "user-1", PaymentStatus.COMPLETED);

        when(jpaRepository.findById(paymentId)).thenReturn(Optional.of(entity));

        Optional<Payment> result = repository.findById(paymentId);

        assertTrue(result.isPresent());
        assertEquals(paymentId, result.get().getId());
        assertEquals(PaymentStatus.COMPLETED, result.get().getStatus());
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        UUID paymentId = UUID.randomUUID();
        when(jpaRepository.findById(paymentId)).thenReturn(Optional.empty());

        Optional<Payment> result = repository.findById(paymentId);

        assertFalse(result.isPresent());
    }

    @Test
    void shouldFindByUserId() {
        String userId = "user-1";
        PaymentEntity e1 = createEntity(UUID.randomUUID(), userId, PaymentStatus.COMPLETED);
        PaymentEntity e2 = createEntity(UUID.randomUUID(), userId, PaymentStatus.PENDING);

        when(jpaRepository.findByUserId(userId)).thenReturn(List.of(e1, e2));

        List<Payment> result = repository.findByUserId(userId);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getUserId().equals(userId)));
    }

    @Test
    void shouldFindByOrderId() {
        UUID orderId = UUID.randomUUID();
        PaymentEntity entity = createEntity(UUID.randomUUID(), "user-1", PaymentStatus.PENDING);
        entity.setOrderId(orderId);

        when(jpaRepository.findByOrderId(orderId)).thenReturn(List.of(entity));

        List<Payment> result = repository.findByOrderId(orderId);

        assertEquals(1, result.size());
        assertEquals(orderId, result.get(0).getOrderId());
    }

    @Test
    void shouldDeletePayment() {
        Payment domain = createPayment(UUID.randomUUID(), "user-1", "PIX", PaymentStatus.FAILED);

        doNothing().when(jpaRepository).deleteById(domain.getId());

        assertDoesNotThrow(() -> repository.delete(domain));
        verify(jpaRepository).deleteById(domain.getId());
    }

    @Test
    void shouldReturnEmptyListWhenNoPaymentsForUser() {
        String userId = "user-1";
        when(jpaRepository.findByUserId(userId)).thenReturn(List.of());

        List<Payment> result = repository.findByUserId(userId);

        assertTrue(result.isEmpty());
    }

    private Payment createPayment(UUID id, String userId, String method, PaymentStatus status) {
        return Payment.builder()
                .id(id)
                .userId(userId)
                .orderId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .paymentMethod(method)
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private PaymentEntity createEntity(UUID id, String userId, PaymentStatus status) {
        return PaymentEntity.builder()
                .id(id)
                .userId(userId)
                .orderId(UUID.randomUUID())
                .amount(new BigDecimal("150.00"))
                .paymentMethod("CREDIT_CARD")
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
