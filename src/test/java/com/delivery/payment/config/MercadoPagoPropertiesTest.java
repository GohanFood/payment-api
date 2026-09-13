package com.delivery.payment.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MercadoPagoPropertiesTest {

    @Test
    void shouldDetectSandboxToken() {
        MercadoPagoProperties properties = new MercadoPagoProperties();
        properties.setAccessToken("TEST-123456");

        assertTrue(properties.isTokenConfigured());
        assertTrue(properties.isSandboxToken());
        assertFalse(properties.isProductionToken());
    }

    @Test
    void shouldDetectProductionToken() {
        MercadoPagoProperties properties = new MercadoPagoProperties();
        properties.setAccessToken("APP_USR-123456");

        assertTrue(properties.isTokenConfigured());
        assertTrue(properties.isProductionToken());
        assertFalse(properties.isSandboxToken());
    }

    @Test
    void shouldDetectProductionEnvironment() {
        MercadoPagoProperties properties = new MercadoPagoProperties();
        properties.setEnvironment("production");

        assertTrue(properties.isProductionEnvironment());
    }

    @Test
    void shouldDetectSandboxEnvironmentByDefault() {
        MercadoPagoProperties properties = new MercadoPagoProperties();

        assertFalse(properties.isProductionEnvironment());
    }

    @Test
    void shouldConsiderEmptyTokenAsNotConfigured() {
        MercadoPagoProperties properties = new MercadoPagoProperties();
        properties.setAccessToken("");

        assertFalse(properties.isTokenConfigured());
        assertFalse(properties.isSandboxToken());
        assertFalse(properties.isProductionToken());
    }
}
