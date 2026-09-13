package com.delivery.payment.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCardRequest {

    /** CardToken gerado pelo MercadoPago.js CardForm (uso único). */
    @NotBlank
    private String cardToken;

    /** Bandeira do cartão (visa, master, elo, amex) — opcional. */
    private String paymentMethodId;
}
