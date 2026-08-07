package com.delivery.payment.application.service;

import com.delivery.payment.application.usecase.ProcessPaymentUseCase;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
import com.delivery.payment.domain.payment.exception.PaymentAlreadyProcessedException;
import com.delivery.payment.domain.payment.exception.PaymentNotFoundException;
import com.delivery.payment.port.PaymentMessagingPort;
import com.delivery.payment.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessPaymentService implements ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentMessagingPort paymentMessagingPort;

    @Override
    @Transactional
    public Payment execute(UUID paymentId, String gatewayToken) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentAlreadyProcessedException(paymentId);
        }

        // Simulação de chamada ao gateway de pagamento
        String transactionId = "GW-" + UUID.randomUUID().toString().substring(0, 12);

        payment.markAsCompleted(transactionId);
        Payment updated = paymentRepository.save(payment);

        paymentMessagingPort.publishPaymentCompleted(updated.getId(), updated.getOrderId());

        return updated;
    }
}
