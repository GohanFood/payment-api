package com.delivery.payment.application.usecase;

public interface DeleteCardUseCase {
    void execute(String customerId, String cardId);
}
