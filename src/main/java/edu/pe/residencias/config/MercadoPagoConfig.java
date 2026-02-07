package edu.pe.residencias.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MercadoPagoConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MercadoPagoConfig.class);

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${mercadopago.notification-url:}")
    private String notificationUrl;

    @PostConstruct
    public void init() {
        // Prepare runtime configuration for Mercado Pago integration.
        // We don't hardcode values here; the token must be provided via properties/env.
        if (accessToken == null || accessToken.isBlank()) {
            LOGGER.warn("Mercado Pago access token is not configured (mercadopago.access-token). Integration will fail until set.");
        } else {
            LOGGER.info("Mercado Pago configured (sandbox/test mode expected). Access token present.");
        }
        if (notificationUrl != null && !notificationUrl.isBlank()) {
            LOGGER.info("Mercado Pago notification URL configured.");
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getNotificationUrl() {
        return notificationUrl;
    }
}
