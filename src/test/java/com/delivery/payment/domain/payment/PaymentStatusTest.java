package com.delivery.payment.domain.payment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentStatusTest {

    @Test
    void shouldHaveAllExpectedValues() {
        PaymentStatus[] values = PaymentStatus.values();
        assertEquals(6, values.length);
    }

    @Test
    void shouldContainPending() {
        assertNotNull(PaymentStatus.valueOf("PENDING"));
        assertEquals(PaymentStatus.PENDING, PaymentStatus.PENDING);
    }

    @Test
    void shouldContainProcessing() {
        assertNotNull(PaymentStatus.valueOf("PROCESSING"));
    }

    @Test
    void shouldContainCompleted() {
        assertNotNull(PaymentStatus.valueOf("COMPLETED"));
    }

    @Test
    void shouldContainFailed() {
        assertNotNull(PaymentStatus.valueOf("FAILED"));
    }

    @Test
    void shouldContainRefunded() {
        assertNotNull(PaymentStatus.valueOf("REFUNDED"));
    }

    @Test
    void shouldContainCancelled() {
        assertNotNull(PaymentStatus.valueOf("CANCELLED"));
    }

    @Test
    void shouldHaveCorrectOrdinalOrder() {
        assertEquals(0, PaymentStatus.PENDING.ordinal());
        assertEquals(1, PaymentStatus.PROCESSING.ordinal());
        assertEquals(2, PaymentStatus.COMPLETED.ordinal());
        assertEquals(3, PaymentStatus.FAILED.ordinal());
        assertEquals(4, PaymentStatus.REFUNDED.ordinal());
        assertEquals(5, PaymentStatus.CANCELLED.ordinal());
    }

    @Test
    void pendingShouldNotBeCompleted() {
        assertNotEquals(PaymentStatus.PENDING, PaymentStatus.COMPLETED);
    }

    @Test
    void valueOfShouldBeCaseSensitive() {
        assertThrows(IllegalArgumentException.class, () -> PaymentStatus.valueOf("pending"));
        assertThrows(IllegalArgumentException.class, () -> PaymentStatus.valueOf("Completed"));
        assertThrows(IllegalArgumentException.class, () -> PaymentStatus.valueOf("INVALID"));
    }
}
