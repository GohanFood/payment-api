package com.delivery.payment.adapter.out.gateway;

public class MercadoPagoIntegrationException extends RuntimeException {

    public MercadoPagoIntegrationException(String message) {
        super(message);
    }

    public MercadoPagoIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
