package com.delivery.payment.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PixPaymentResponse {

    private Long mpPaymentId;
    private String status;
    private String qrCode;
    private String qrCodeBase64;
    private String ticketUrl;
    private OffsetDateTime dateOfExpiration;
}
