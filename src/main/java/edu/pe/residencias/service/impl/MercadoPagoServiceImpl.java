package edu.pe.residencias.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.pe.residencias.config.MercadoPagoConfig;
import edu.pe.residencias.dto.payment.CreatePaymentRequest;
import edu.pe.residencias.dto.payment.CreatePaymentResponse;
import edu.pe.residencias.exception.MercadoPagoException;
import edu.pe.residencias.service.MercadoPagoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class MercadoPagoServiceImpl implements MercadoPagoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MercadoPagoServiceImpl.class);

    private final MercadoPagoConfig config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final edu.pe.residencias.service.ResidenciaService residenciaService;

    public MercadoPagoServiceImpl(MercadoPagoConfig config, ObjectMapper objectMapper, edu.pe.residencias.service.ResidenciaService residenciaService) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.residenciaService = residenciaService;
    }

    @Override
    public CreatePaymentResponse createPreference(CreatePaymentRequest request) {
        try {
            String token = config.getAccessToken();
            if (token == null || token.isBlank()) {
                throw new MercadoPagoException("Mercado Pago access token not configured");
            }

            Map<String, Object> item = new HashMap<>();
            item.put("title", request.getTitle());
            item.put("quantity", request.getQuantity());
            item.put("unit_price", request.getPrice());
            item.put("currency_id", "PEN");

            Map<String, Object> body = new HashMap<>();
            body.put("items", new Object[]{item});

            // Link preference to residencia using external_reference so webhook/checks can find it
            if (request.getResidenciaId() != null) {
                body.put("external_reference", String.valueOf(request.getResidenciaId()));
            }

            String notificationUrl = config.getNotificationUrl();
            if (notificationUrl != null && !notificationUrl.isBlank()) {
                body.put("notification_url", notificationUrl);
            }

            String requestBody = objectMapper.writeValueAsString(body);

            // Structured logging of request details (only if present)
            try {
                JsonNode reqJson = objectMapper.readTree(requestBody);
                String transactionAmount = reqJson.has("transaction_amount") ? reqJson.get("transaction_amount").asText() : null;
                String paymentMethodId = reqJson.has("payment_method_id") ? reqJson.get("payment_method_id").asText() : null;
                String installments = reqJson.has("installments") ? reqJson.get("installments").asText() : null;
                String payerEmail = null;
                String payerIdType = null;
                String payerIdNumber = null;
                if (reqJson.has("payer")) {
                    JsonNode payer = reqJson.get("payer");
                    payerEmail = payer.has("email") ? payer.get("email").asText(null) : null;
                    if (payer.has("identification")) {
                        JsonNode idn = payer.get("identification");
                        payerIdType = idn.has("type") ? idn.get("type").asText(null) : null;
                        payerIdNumber = idn.has("number") ? idn.get("number").asText(null) : null;
                    }
                }

                String tokenPreview = token == null ? null : token.substring(0, Math.min(15, token.length()));

                StringBuilder sb = new StringBuilder();
                sb.append("[MP REQUEST]\n");
                if (transactionAmount != null) sb.append("transaction_amount=").append(transactionAmount).append("\n");
                if (paymentMethodId != null) sb.append("payment_method_id=").append(paymentMethodId).append("\n");
                if (installments != null) sb.append("installments=").append(installments).append("\n");
                if (payerEmail != null) sb.append("payer.email=").append(payerEmail).append("\n");
                if (payerIdType != null) sb.append("payer.identification.type=").append(payerIdType).append("\n");
                if (payerIdNumber != null) sb.append("payer.identification.number=").append(payerIdNumber).append("\n");
                if (tokenPreview != null) sb.append("token_preview=").append(tokenPreview).append("\n");
                LOGGER.info(sb.toString());
            } catch (Exception ex) {
                LOGGER.warn("Failed to extract MP request fields for logging", ex);
            }

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mercadopago.com/checkout/preferences"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response;
            try {
                response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            } catch (Exception ex) {
                LOGGER.error("Error sending request to Mercado Pago", ex);
                throw ex;
            }

            int status = response.statusCode();
            String bodyResp = response.body();

            // Log full response JSON and key fields if present
            try {
                StringBuilder sbResp = new StringBuilder();
                sbResp.append("[MP RESPONSE]\n");
                if (bodyResp != null) {
                    JsonNode respJson = objectMapper.readTree(bodyResp);
                    String respStatus = respJson.has("status") ? respJson.get("status").asText(null) : null;
                    String respStatusDetail = respJson.has("status_detail") ? respJson.get("status_detail").asText(null) : null;
                    String respId = respJson.has("id") ? respJson.get("id").asText(null) : null;
                    if (respId != null) sbResp.append("id=").append(respId).append("\n");
                    if (respStatus != null) sbResp.append("status=").append(respStatus).append("\n");
                    if (respStatusDetail != null) sbResp.append("status_detail=").append(respStatusDetail).append("\n");
                    sbResp.append("body=").append(bodyResp).append("\n");
                } else {
                    sbResp.append("body=null\n");
                }
                LOGGER.info(sbResp.toString());
            } catch (Exception ex) {
                LOGGER.warn("Failed to parse Mercado Pago response for structured logging", ex);
                LOGGER.info("[MP RESPONSE] raw body={}", bodyResp);
            }

            if (status < 200 || status >= 300) {
                // Log error body explicitly then throw
                LOGGER.error("MercadoPago create preference failed: status={} body={}", status, bodyResp);
                throw new MercadoPagoException("Error creating Mercado Pago preference: status=" + status + " body=" + bodyResp);
            }

            JsonNode json = objectMapper.readTree(bodyResp);
            String initPoint = json.has("init_point") ? json.get("init_point").asText(null) : null;
            String prefId = json.has("id") ? json.get("id").asText(null) : null;

            if (initPoint == null) {
                LOGGER.error("MercadoPago response missing init_point: {}", bodyResp);
                throw new MercadoPagoException("Invalid response from Mercado Pago: missing init_point");
            }

            return new CreatePaymentResponse(initPoint, prefId);

        } catch (MercadoPagoException ex) {
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("Unexpected error while creating Mercado Pago preference", ex);
            throw new MercadoPagoException("Unexpected error while creating preference", ex);
        }
    }

    private String getAccessTokenHeader() {
        return "Bearer " + config.getAccessToken();
    }

    @Override
    public boolean verifyPreferenceAndMarkResidencia(String preferenceId, Long residenciaId) {
        try {
            if (preferenciaPagada(preferenceId)) {
                if (residenciaId != null) {
                    var opt = residenciaService.read(residenciaId);
                    if (opt.isPresent()) {
                        var r = opt.get();
                        r.setDestacado(true);
                        r.setDestacadoFecha(java.time.LocalDateTime.now());
                        residenciaService.update(r);
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception ex) {
            LOGGER.error("Error verifying preference and marking residencia", ex);
            return false;
        }
    }

    private boolean preferenciaPagada(String preferenceId) throws Exception {
        if (preferenceId == null || preferenceId.isBlank()) return false;
        String url = "https://api.mercadopago.com/v1/payments/search?preference_id=" + preferenceId;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", getAccessTokenHeader())
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode results = root.has("results") ? root.get("results") : null;
            if (results != null && results.isArray()) {
                for (JsonNode p : results) {
                    String status = p.has("status") ? p.get("status").asText("") : "";
                    if ("approved".equalsIgnoreCase(status)) return true;
                }
            }
        } else {
            LOGGER.warn("Preference payments search returned status {} body={}", resp.statusCode(), resp.body());
        }
        return false;
    }

    @Override
    public boolean handlePaymentNotification(String paymentId) {
        try {
            if (paymentId == null || paymentId.isBlank()) return false;
            // Get payment details
            String url = "https://api.mercadopago.com/v1/payments/" + paymentId;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", getAccessTokenHeader())
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                LOGGER.warn("Failed to fetch payment {}: status={} body={}", paymentId, resp.statusCode(), resp.body());
                return false;
            }
            JsonNode paymentJson = objectMapper.readTree(resp.body());
            String status = paymentJson.has("status") ? paymentJson.get("status").asText("") : "";
            if (!"approved".equalsIgnoreCase(status)) return false;

            // Try to find preference_id from payment
            String preferenceId = null;
            if (paymentJson.has("preference_id")) preferenceId = paymentJson.get("preference_id").asText(null);
            if (preferenceId == null) {
                // fallback: some responses may have order -> preference_id
                if (paymentJson.has("order") && paymentJson.get("order").has("preference_id")) {
                    preferenceId = paymentJson.get("order").get("preference_id").asText(null);
                }
            }

            if (preferenceId == null) {
                LOGGER.warn("Payment {} approved but no preference_id found", paymentId);
                return false;
            }

            // Get preference to read external_reference
            String prefUrl = "https://api.mercadopago.com/checkout/preferences/" + preferenceId;
            HttpRequest prefReq = HttpRequest.newBuilder()
                    .uri(URI.create(prefUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", getAccessTokenHeader())
                    .GET()
                    .build();
            HttpResponse<String> prefResp = httpClient.send(prefReq, HttpResponse.BodyHandlers.ofString());
            if (prefResp.statusCode() < 200 || prefResp.statusCode() >= 300) {
                LOGGER.warn("Failed to fetch preference {}: status={} body={}", preferenceId, prefResp.statusCode(), prefResp.body());
                return false;
            }
            JsonNode prefJson = objectMapper.readTree(prefResp.body());
            String externalRef = prefJson.has("external_reference") ? prefJson.get("external_reference").asText(null) : null;
            if (externalRef == null) {
                LOGGER.warn("Preference {} has no external_reference", preferenceId);
                return false;
            }
            Long residenciaId = null;
            try { residenciaId = Long.valueOf(externalRef); } catch (Exception ex) { LOGGER.warn("Invalid external_reference {}", externalRef); }
            if (residenciaId == null) return false;

            var opt = residenciaService.read(residenciaId);
            if (opt.isPresent()) {
                var r = opt.get();
                r.setDestacado(true);
                r.setDestacadoFecha(java.time.LocalDateTime.now());
                residenciaService.update(r);
                return true;
            }
            return false;
        } catch (Exception ex) {
            LOGGER.error("Error handling payment notification", ex);
            return false;
        }
    }
}
