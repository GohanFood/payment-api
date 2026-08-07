package com.delivery.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propriedades de configuração do Mercado Pago.
 *
 * Variáveis de ambiente:
 * - MERCADOPAGO_ACCESS_TOKEN: token de acesso da conta Mercado Pago (obrigatório)
 * - MERCADOPAGO_PUBLIC_KEY: chave pública usada no frontend (MercadoPago.js)
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "mercadopago")
public class MercadoPagoProperties {

    /** Access Token do Mercado Pago (produção ou sandbox) */
    private String accessToken;

    /** Public Key usada no frontend para inicializar o MercadoPago.js */
    private String publicKey;
}
