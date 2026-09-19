package com.delivery.payment.adapter.in.web;

import com.delivery.payment.application.dto.request.AddCardRequest;
import com.delivery.payment.application.dto.request.CreateCustomerRequest;
import com.delivery.payment.application.dto.response.AddCardResponse;
import com.delivery.payment.application.dto.response.CreateCustomerResponse;
import com.delivery.payment.application.usecase.AddCardUseCase;
import com.delivery.payment.application.usecase.CreateCustomerUseCase;
import com.delivery.payment.application.usecase.DeleteCardUseCase;
import com.delivery.payment.domain.customer.CustomerCardReference;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CreateCustomerUseCase createCustomerUseCase;
    private final AddCardUseCase addCardUseCase;
    private final DeleteCardUseCase deleteCardUseCase;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CreateCustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerCardReference reference = createCustomerUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toCreateResponse(reference));
    }

    @PostMapping("/{customerId}/cards")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AddCardResponse> addCard(
            @PathVariable String customerId,
            @Valid @RequestBody AddCardRequest request) {
        CustomerCardReference reference = addCardUseCase.execute(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toAddCardResponse(reference));
    }

    @DeleteMapping("/{customerId}/cards/{cardId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteCard(
            @PathVariable String customerId,
            @PathVariable String cardId) {
        deleteCardUseCase.execute(customerId, cardId);
        return ResponseEntity.noContent().build();
    }

    private CreateCustomerResponse toCreateResponse(CustomerCardReference reference) {
        return CreateCustomerResponse.builder()
                .customerId(reference.getCustomerId())
                .cardId(reference.getCardId())
                .paymentMethodId(reference.getPaymentMethodId())
                .build();
    }

    private AddCardResponse toAddCardResponse(CustomerCardReference reference) {
        return AddCardResponse.builder()
                .cardId(reference.getCardId())
                .paymentMethodId(reference.getPaymentMethodId())
                .build();
    }
}
