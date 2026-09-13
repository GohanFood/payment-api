package com.delivery.payment.application.service;

import com.delivery.payment.application.usecase.RefundPaymentUseCase;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.domain.payment.PaymentStatus;
import com.delivery.payment.domain.payment.exception.PaymentAlreadyProcessedException;
import com.delivery.payment.domain.payment.exception.PaymentNotFoundException;
import com.delivery.payment.port.PaymentMessagingPort;
import com.delivery.payment.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundPaymentService implements RefundPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentMessagingPort paymentMessagingPort;

    @Override
    @Transactional
    public Payment execute(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentAlreadyProcessedException(paymentId);
        }

        payment.markAsRefunded();
        Payment updated = paymentRepository.save(payment);

        paymentMessagingPort.publishPaymentRefunded(updated.getId(), updated.getReferenceId());
        log.info("Pagamento reembolsado: paymentId={}, referenceId={}", paymentId, updated.getReferenceId());

        return updated;
    }
}
