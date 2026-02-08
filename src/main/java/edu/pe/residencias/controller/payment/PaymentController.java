package edu.pe.residencias.controller.payment;

import edu.pe.residencias.dto.payment.CreatePaymentRequest;
import edu.pe.residencias.dto.payment.CreatePaymentResponse;
import edu.pe.residencias.service.MercadoPagoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import edu.pe.residencias.service.WebhookService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentController.class);

    private final MercadoPagoService mercadoPagoService;
    private final WebhookService webhookService;

    public PaymentController(MercadoPagoService mercadoPagoService, WebhookService webhookService) {
        this.mercadoPagoService = mercadoPagoService;
        this.webhookService = webhookService;
    }

    @PostMapping(path = "/create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreatePaymentResponse> create(@Valid @RequestBody CreatePaymentRequest request) {
        CreatePaymentResponse response = mercadoPagoService.createPreference(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> webhook(HttpServletRequest servletRequest, @RequestBody String payload) {
        // Basic webhook receiver: try to extract payment id and delegate to service.
        LOGGER.info("Received Mercado Pago webhook: {}", payload);
        // Log full payload
        LOGGER.info("Webhook payload: {}", payload);
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = om.readTree(payload);
            String type = null;
            if (root.has("topic")) type = root.get("topic").asText(null);
            if (type == null && root.has("type")) type = root.get("type").asText(null);

            String resource = root.has("resource") ? root.get("resource").asText(null) : null;
            String resourceId = null;
            if (root.has("data") && root.get("data").has("id")) {
                resourceId = root.get("data").get("id").asText(null);
            } else if (root.has("id")) {
                resourceId = root.get("id").asText(null);
            }

            // If merchant_order topic provides resource field, prefer it
            String merchantOrderId = null;
            if ("merchant_order".equalsIgnoreCase(type) && resource != null) merchantOrderId = resource;
            else merchantOrderId = resourceId;

            // Log required fields for merchant_order topic
            if ("merchant_order".equalsIgnoreCase(type)) {
                String timestamp = java.time.Instant.now().toString();
                LOGGER.info("[MP WEBHOOK] topic={} resource={} merchant_order_id={} timestamp={}", type, resource, merchantOrderId, timestamp);
            }

            LOGGER.info("Webhook extracted - type='{}' data.id='{}' resource='{}'", type, resourceId, resource);

            if ("payment".equalsIgnoreCase(type) && resourceId != null) {
                webhookService.processPaymentNotification(resourceId);
            } else if ("merchant_order".equalsIgnoreCase(type) && merchantOrderId != null) {
                webhookService.processMerchantOrderNotification(merchantOrderId);
            }
        } catch (Exception ex) {
            LOGGER.warn("Error parsing webhook payload", ex);
        }

        // Always return 200 immediately
        return ResponseEntity.ok("received");
    }

    @GetMapping(path = "/check")
    public ResponseEntity<Object> checkPayment(String preferenceId, Long residenciaId) {
        if ((preferenceId == null || preferenceId.isBlank()) && residenciaId == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "preferenceId or residenciaId required"));
        }
        boolean ok = false;
        try {
            if (preferenceId != null && !preferenceId.isBlank() && residenciaId != null) {
                ok = mercadoPagoService.verifyPreferenceAndMarkResidencia(preferenceId, residenciaId);
            } else if (preferenceId != null && !preferenceId.isBlank()) {
                // If residenciaId not provided, only check payment status
                ok = mercadoPagoService.verifyPreferenceAndMarkResidencia(preferenceId, null);
            }
            return ResponseEntity.ok(java.util.Map.of("paid", ok));
        } catch (Exception ex) {
            LOGGER.error("Error checking payment", ex);
            return ResponseEntity.status(500).body(java.util.Map.of("error", "internal"));
        }
    }
}
