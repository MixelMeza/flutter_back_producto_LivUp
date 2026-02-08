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
        if (accessToken == null || accessToken.isBlank()) {
            LOGGER.warn("Mercado Pago access token is not configured (mercadopago.access-token). Integration will fail until set.");
            return;
        }

        // Log first 10 characters only
        String preview = accessToken.substring(0, Math.min(10, accessToken.length()));
        LOGGER.info("Mercado Pago access token present (preview={})", preview);

        // Try to initialize official SDK if present (best-effort). Use reflection to avoid hard dependency issues.
        try {
            // Common SDK class names: com.mercadopago.MercadoPagoConfig or com.mercadopago.MercadoPago
            try {
                Class<?> cls = Class.forName("com.mercadopago.MercadoPagoConfig");
                try {
                    java.lang.reflect.Method m = cls.getMethod("setAccessToken", String.class);
                    m.invoke(null, accessToken);
                    LOGGER.info("Initialized Mercado Pago SDK via com.mercadopago.MercadoPagoConfig.setAccessToken(...)");
                } catch (NoSuchMethodException e) {
                    LOGGER.debug("com.mercadopago.MercadoPagoConfig exists but no setAccessToken method");
                }
            } catch (ClassNotFoundException e) {
                // try alternative
                try {
                    Class<?> cls2 = Class.forName("com.mercadopago.MercadoPago");
                    java.lang.reflect.Method m2 = cls2.getMethod("setAccessToken", String.class);
                    m2.invoke(null, accessToken);
                    LOGGER.info("Initialized Mercado Pago SDK via com.mercadopago.MercadoPago.setAccessToken(...)");
                } catch (ClassNotFoundException | NoSuchMethodException e2) {
                    LOGGER.debug("Mercado Pago SDK not available on classpath or method not found; continuing with RestTemplate-based calls.");
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to initialize Mercado Pago SDK automatically", t);
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
