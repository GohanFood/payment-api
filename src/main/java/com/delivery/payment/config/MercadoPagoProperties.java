package com.delivery.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propriedades de configuração do Mercado Pago — exclusivamente Sandbox.
 *
 * <p>Esta API está configurada para operar SOMENTE no ambiente Sandbox do Mercado Pago.
 * Tokens de produção (prefixo {@code APP_USR-}) são rejeitados na inicialização.</p>
 *
 * <h3>Credenciais Sandbox</h3>
 * Gere em: <a href="https://www.mercadopago.com.br/settings/account/credentials">Credentials Mercado Pago</a>
 * <ul>
 *   <li>Access Token de teste: prefixo {@code TEST-}</li>
 *   <li>Public Key de teste: prefixo {@code TEST-}</li>
 *   <li>Cartões de teste: <a href="https://www.mercadopago.com.br/developers/pt/docs/checkout-api-payments/additional-content/your-integrations/test/cards">Cartões de teste</a></li>
 * </ul>
 *
 * <p>Variáveis de ambiente:</p>
 * <ul>
 *   <li>{@code MERCADOPAGO_ENVIRONMENT} — fixo como {@code sandbox}</li>
 *   <li>{@code MERCADOPAGO_ACCESS_TOKEN} — token de acesso sandbox (prefixo TEST-)</li>
 *   <li>{@code MERCADOPAGO_PUBLIC_KEY} — chave pública sandbox</li>
 *   <li>{@code MERCADOPAGO_WEBHOOK_SECRET} — chave secreta para validação de assinatura de webhooks</li>
 * </ul>
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "mercadopago")
public class MercadoPagoProperties {

    /** Ambiente do Mercado Pago — fixo como sandbox. */
    private String environment = "sandbox";

    /**
     * Access Token do Mercado Pago Sandbox (prefixo {@code TEST-}).
     * Obrigatório para chamar a API real do Mercado Pago.
     */
    private String accessToken;

    /** Public Key Sandbox usada no frontend para inicializar o MercadoPago.js */
    private String publicKey;

    /**
     * Secret key para validação de assinatura de webhooks (HMAC-SHA256).
     * <p>
     * Quando não configurada (vazia/null), a validação é desabilitada —
     * útil para ambientes de desenvolvimento/teste. Em produção, é obrigatória.
     */
    private String webhookSecret;

    /** Retorna {@code true} se o access token está configurado e parece ser de sandbox. */
    public boolean isTokenConfigured() {
        return accessToken != null && !accessToken.isBlank();
    }

    /** Retorna {@code true} se o token tem prefixo de sandbox ({@code TEST-}). */
    public boolean isSandboxToken() {
        return isTokenConfigured() && accessToken.startsWith("TEST-");
    }

    /** Retorna {@code true} se o token tem prefixo de produção ({@code APP_USR-}). */
    public boolean isProductionToken() {
        return isTokenConfigured() && accessToken.startsWith("APP_USR-");
    }

    /** Retorna {@code true} se o ambiente configurado é produção. */
    public boolean isProductionEnvironment() {
        return "production".equalsIgnoreCase(environment);
    }
}
