package com.delivery.payment.domain.payment.exception;

/**
 * Exception lançada quando o gateway de pagamento externo está indisponível.
 */
public class GatewayUnavailableException extends RuntimeException {

    public GatewayUnavailableException(String message) {
        super(message);
    }

    public GatewayUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
