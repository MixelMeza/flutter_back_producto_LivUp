package edu.pe.residencias.service.impl;

import edu.pe.residencias.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import edu.pe.residencias.service.MercadoPagoService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class WebhookServiceImpl implements WebhookService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookServiceImpl.class);

    private final RestTemplate restTemplate;
    private final MercadoPagoService mercadoPagoService;

    private final String accessToken;

    public WebhookServiceImpl(RestTemplate restTemplate,
                              MercadoPagoService mercadoPagoService,
                              @Value("${mercadopago.access-token:}") String accessToken) {
        this.restTemplate = restTemplate;
        this.mercadoPagoService = mercadoPagoService;
        this.accessToken = accessToken;
    }

    @Override
    public void processPaymentNotification(String paymentId) {
        try {
            if (paymentId == null || paymentId.isBlank()) {
                LOGGER.warn("processPaymentNotification called with empty paymentId");
                return;
            }

            String url = "https://api.mercadopago.com/v1/payments/" + paymentId;

            HttpHeaders headers = new HttpHeaders();
            if (accessToken != null && !accessToken.isBlank()) {
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            }
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            if (resp != null) {
                String body = resp.getBody();
                LOGGER.info("MercadoPago payment details for id={}: status={}, body={}", paymentId, resp.getStatusCode(), body);
                try {
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode json = om.readTree(body == null ? "{}" : body);
                    String status = json.has("status") ? json.get("status").asText("") : null;
                    String statusDetail = json.has("status_detail") ? json.get("status_detail").asText("") : null;
                    LOGGER.info("Payment {} - status='{}' status_detail='{}'", paymentId, status, statusDetail);
                } catch (Exception ex) {
                    LOGGER.warn("Failed to parse payment JSON for id={}", paymentId, ex);
                }
            }
        } catch (RestClientException ex) {
            // Log and swallow: webhook must not return 500
            LOGGER.warn("Error calling Mercado Pago API for payment {}: {}", paymentId, ex.getMessage());
        } catch (Exception ex) {
            LOGGER.error("Unexpected error processing payment notification {}", paymentId, ex);
        }
    }

    @Override
    public void processMerchantOrderNotification(String merchantOrderId) {
        try {
            if (merchantOrderId == null || merchantOrderId.isBlank()) {
                LOGGER.warn("processMerchantOrderNotification called with empty id");
                return;
            }
            Long mid = null;
            try { mid = Long.valueOf(merchantOrderId); } catch (Exception e) { LOGGER.warn("Invalid merchant_order id {}", merchantOrderId); return; }

            // Delegate to MercadoPagoServiceImpl which contains the detailed processing/logging
            try {
                mercadoPagoService.processMerchantOrder(mid);
            } catch (Exception ex) {
                LOGGER.error("Error delegating merchant_order {} to MercadoPagoService: {}", merchantOrderId, ex.getMessage());
            }

        } catch (Exception ex) {
            LOGGER.error("Unexpected error in processMerchantOrderNotification {}", merchantOrderId, ex);
        }
    }
}
