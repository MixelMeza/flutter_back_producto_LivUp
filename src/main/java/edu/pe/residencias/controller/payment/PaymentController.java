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

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentController.class);

    private final MercadoPagoService mercadoPagoService;

    public PaymentController(MercadoPagoService mercadoPagoService) {
        this.mercadoPagoService = mercadoPagoService;
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
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = om.readTree(payload);
            String paymentId = null;
            // common formats: {"data": {"id": "123"}} or {"type": "payment", "data": {"id": "123"}}
            if (root.has("data") && root.get("data").has("id")) {
                paymentId = root.get("data").get("id").asText(null);
            } else if (root.has("id")) {
                paymentId = root.get("id").asText(null);
            }
            if (paymentId != null) {
                boolean handled = mercadoPagoService.handlePaymentNotification(paymentId);
                if (handled) return ResponseEntity.ok("handled");
            }
        } catch (Exception ex) {
            LOGGER.warn("Error processing webhook payload", ex);
        }
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
