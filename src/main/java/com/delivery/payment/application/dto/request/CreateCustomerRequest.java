package com.delivery.payment.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequest {

    @NotBlank
    @Email
    private String email;

    private String firstName;

    private String lastName;

    private String documentType;

    private String documentNumber;

    /** CardToken gerado pelo MercadoPago.js CardForm (uso único). */
    @NotBlank
    private String cardToken;

    /** Bandeira do cartão (visa, master, elo, amex) — opcional. */
    private String paymentMethodId;
}
