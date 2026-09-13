package com.delivery.payment.port;

import com.delivery.payment.domain.payment.Payment;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    List<Payment> findByUserId(String userId);

    /**
     * Busca paginada de pagamentos por usuário.
     * @param userId ID do usuário
     * @param page número da página (0-based)
     * @param size tamanho da página
     * @return lista de pagamentos da página solicitada
     */
    List<Payment> findByUserId(String userId, int page, int size);

    /**
     * Conta total de pagamentos de um usuário (útil para paginação).
     */
    long countByUserId(String userId);

    List<Payment> findByReferenceId(String referenceId);

    Optional<Payment> findByMpPaymentId(Long mpPaymentId);

    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);

    List<Payment> findAll();

    void delete(Payment payment);
}
