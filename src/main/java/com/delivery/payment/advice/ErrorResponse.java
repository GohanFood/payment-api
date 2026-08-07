package com.delivery.payment.advice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;

    @Data
    @Builder
    @AllArgsConstructor
    public static class ErrorDetail {
        private String field;
        private String message;
    }
}
