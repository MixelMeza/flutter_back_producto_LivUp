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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class MercadoPagoServiceImpl implements MercadoPagoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MercadoPagoServiceImpl.class);

    private final MercadoPagoConfig config;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final edu.pe.residencias.service.ResidenciaService residenciaService;

    public MercadoPagoServiceImpl(MercadoPagoConfig config, ObjectMapper objectMapper, RestTemplate restTemplate, edu.pe.residencias.service.ResidenciaService residenciaService) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
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

            String url = "https://api.mercadopago.com/checkout/preferences";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            HttpEntity<String> reqEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> respEntity = restTemplate.exchange(url, HttpMethod.POST, reqEntity, String.class);
            int status = respEntity.getStatusCodeValue();
            String bodyResp = respEntity.getBody();

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
                LOGGER.error("MercadoPago create preference failed: status={} body={}", status, bodyResp);
                throw new MercadoPagoException("Error creating Mercado Pago preference: status=" + status + " body=" + bodyResp);
            }

            JsonNode json = objectMapper.readTree(bodyResp == null ? "{}" : bodyResp);
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
    public com.fasterxml.jackson.databind.JsonNode getMerchantOrder(Long id) {
        try {
            String url = "https://api.mercadopago.com/merchant_orders/" + id;
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, getAccessTokenHeader());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String body = resp.getBody();
            return objectMapper.readTree(body == null ? "{}" : body);
        } catch (HttpStatusCodeException hex) {
            LOGGER.error("Error fetching merchant order {}: body={}", id, hex.getResponseBodyAsString());
            return null;
        } catch (Exception ex) {
            LOGGER.error("Unexpected error fetching merchant order {}", id, ex);
            return null;
        }
    }

    @Override
    public com.fasterxml.jackson.databind.JsonNode getPayment(Long paymentId) {
        try {
            String url = "https://api.mercadopago.com/v1/payments/" + paymentId;
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, getAccessTokenHeader());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String body = resp.getBody();
            return objectMapper.readTree(body == null ? "{}" : body);
        } catch (HttpStatusCodeException hex) {
            LOGGER.error("Error fetching payment {}: body={}", paymentId, hex.getResponseBodyAsString());
            return null;
        } catch (Exception ex) {
            LOGGER.error("Unexpected error fetching payment {}", paymentId, ex);
            return null;
        }
    }

    @Override
    public CreatePaymentResponse createCheckoutPreference(BigDecimal amount, String title, String externalReference, String payerEmail, String dni) {
        try {
            String token = config.getAccessToken();
            if (token == null || token.isBlank()) {
                throw new MercadoPagoException("Mercado Pago access token not configured");
            }

            Map<String, Object> item = new HashMap<>();
            item.put("title", title);
            item.put("quantity", 1);
            item.put("currency_id", "PEN");
            item.put("unit_price", amount);

            Map<String, Object> body = new HashMap<>();
            body.put("items", new Object[]{item});
            if (externalReference != null && !externalReference.isBlank()) body.put("external_reference", externalReference);

            // payer only if provided
            if ((payerEmail != null && !payerEmail.isBlank()) || (dni != null && !dni.isBlank())) {
                Map<String, Object> payer = new HashMap<>();
                if (payerEmail != null && !payerEmail.isBlank()) payer.put("email", payerEmail);
                if (dni != null && !dni.isBlank()) {
                    Map<String, Object> id = new HashMap<>();
                    id.put("type", "DNI");
                    id.put("number", dni);
                    payer.put("identification", id);
                }
                body.put("payer", payer);
            }

            // back_urls and auto_return
            Map<String, String> backUrls = new HashMap<>();
            String notif = config.getNotificationUrl();
            backUrls.put("success", notif);
            backUrls.put("failure", notif);
            backUrls.put("pending", notif);
            body.put("back_urls", backUrls);
            body.put("auto_return", "approved");

            String notificationUrl = config.getNotificationUrl();
            if (notificationUrl != null && !notificationUrl.isBlank()) body.put("notification_url", notificationUrl);

            // Log request summary
            try {
                StringBuilder reqLog = new StringBuilder();
                reqLog.append("[MP REQUEST]\n");
                if (externalReference != null) reqLog.append("externalReference=").append(externalReference).append("\n");
                if (amount != null) reqLog.append("amount=").append(amount).append("\n");
                LOGGER.info(reqLog.toString());
            } catch (Exception ex) { LOGGER.warn("Failed logging MP request summary", ex); }

            String url = "https://api.mercadopago.com/checkout/preferences";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            String requestBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> reqEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> respEntity = restTemplate.exchange(url, HttpMethod.POST, reqEntity, String.class);
            String respBody = respEntity.getBody();

            // Log response
            try {
                JsonNode respJson = objectMapper.readTree(respBody == null ? "{}" : respBody);
                String prefId = respJson.has("id") ? respJson.get("id").asText(null) : null;
                String initPoint = respJson.has("init_point") ? respJson.get("init_point").asText(null) : null;
                String sandboxInit = respJson.has("sandbox_init_point") ? respJson.get("sandbox_init_point").asText(null) : null;
                StringBuilder respLog = new StringBuilder();
                respLog.append("[MP RESPONSE]\n");
                if (prefId != null) respLog.append("preferenceId=").append(prefId).append("\n");
                if (initPoint != null) respLog.append("initPoint=").append(initPoint).append("\n");
                LOGGER.info(respLog.toString());
                return new CreatePaymentResponse(initPoint, prefId);
            } catch (Exception ex) {
                LOGGER.warn("Failed to parse MP preference response", ex);
                LOGGER.info("[MP RESPONSE] raw body={}", respBody);
                JsonNode respJson = objectMapper.readTree(respBody == null ? "{}" : respBody);
                String initPoint = respJson.has("init_point") ? respJson.get("init_point").asText(null) : null;
                String prefId = respJson.has("id") ? respJson.get("id").asText(null) : null;
                return new CreatePaymentResponse(initPoint, prefId);
            }
        } catch (HttpStatusCodeException hex) {
            LOGGER.error("MercadoPago error body={}", hex.getResponseBodyAsString());
            throw new MercadoPagoException("Error creating preference: " + hex.getStatusCode(), hex);
        } catch (Exception ex) {
            LOGGER.error("Unexpected error creating checkout preference", ex);
            throw new MercadoPagoException("Unexpected error", ex);
        }
    }

    @Override
    public void processMerchantOrder(Long merchantOrderId) {
        String prefixMO = "[MP MERCHANT ORDER]";
        String prefixPayment = "[MP PAYMENT]";
        if (merchantOrderId == null) {
            LOGGER.warn("{} merchantOrderId is null", prefixMO);
            return;
        }

        String url = "https://api.mercadopago.com/merchant_orders/" + merchantOrderId;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, getAccessTokenHeader());
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            int status = resp.getStatusCodeValue();
            String body = resp.getBody();

            // Log full response JSON
            LOGGER.info("{} Response status={}", prefixMO, status);
            LOGGER.info("{} Full response: {}", prefixMO, body == null ? "null" : body);

            com.fasterxml.jackson.databind.JsonNode mo = objectMapper.readTree(body == null ? "{}" : body);
            com.fasterxml.jackson.databind.JsonNode payments = mo.has("payments") ? mo.get("payments") : null;
            String externalRef = mo.has("external_reference") ? mo.get("external_reference").asText(null) : null;

            LOGGER.info("{} external_reference={}", prefixMO, externalRef);
            LOGGER.info("{} payments array={}", prefixMO, payments == null ? "null" : payments.toString());

            if (payments != null && payments.isArray() && payments.size() > 0) {
                for (com.fasterxml.jackson.databind.JsonNode p : payments) {
                    String pid = p.has("id") ? p.get("id").asText(null) : null;
                    if (pid == null) continue;
                    // Fetch payment details
                    String pUrl = "https://api.mercadopago.com/v1/payments/" + pid;
                    try {
                        ResponseEntity<String> presp = restTemplate.exchange(pUrl, HttpMethod.GET, entity, String.class);
                        int pStatus = presp.getStatusCodeValue();
                        String pBody = presp.getBody();
                        LOGGER.info("{} Response status={} for payment {}", prefixPayment, pStatus, pid);
                        LOGGER.info("{} Full payment response: {}", prefixPayment, pBody == null ? "null" : pBody);

                        com.fasterxml.jackson.databind.JsonNode payJson = objectMapper.readTree(pBody == null ? "{}" : pBody);
                        String paymentStatus = payJson.has("status") ? payJson.get("status").asText(null) : null;
                        String statusDetail = payJson.has("status_detail") ? payJson.get("status_detail").asText(null) : null;
                        String transactionAmount = payJson.has("transaction_amount") ? payJson.get("transaction_amount").asText(null) : null;
                        String paymentMethodId = payJson.has("payment_method_id") ? payJson.get("payment_method_id").asText(null) : null;
                        String dateCreated = payJson.has("date_created") ? payJson.get("date_created").asText(null) : null;
                        String dateApproved = payJson.has("date_approved") ? payJson.get("date_approved").asText(null) : null;
                        String payerEmail = null;
                        if (payJson.has("payer") && payJson.get("payer").has("email")) payerEmail = payJson.get("payer").get("email").asText(null);
                        String metadata = payJson.has("metadata") ? payJson.get("metadata").toString() : null;
                        String prefExternalRef = payJson.has("external_reference") ? payJson.get("external_reference").asText(null) : null;

                        LOGGER.info("{} payment.id={} status={} status_detail={} transaction_amount={} payment_method_id={} date_created={} date_approved={} payer.email={} metadata={} external_reference={}",
                                prefixPayment, pid, paymentStatus, statusDetail, transactionAmount, paymentMethodId, dateCreated, dateApproved, payerEmail, metadata, prefExternalRef);

                        if ("approved".equalsIgnoreCase(paymentStatus)) {
                            LOGGER.info("{} PAYMENT APPROVED for order {}", prefixPayment, externalRef);
                        } else if ("rejected".equalsIgnoreCase(paymentStatus)) {
                            LOGGER.info("{} PAYMENT REJECTED", prefixPayment);
                            LOGGER.info("{} REJECTION DETAIL: {}", prefixPayment, statusDetail);
                        }

                        // Optionally mark residencia if preference available
                        String prefId = null;
                        if (payJson.has("preference_id")) prefId = payJson.get("preference_id").asText(null);
                        if (prefId == null && payJson.has("order") && payJson.get("order").has("preference_id")) {
                            prefId = payJson.get("order").get("preference_id").asText(null);
                        }
                        if (prefId != null && externalRef != null) {
                            try {
                                Long residenciaId = null;
                                try { residenciaId = Long.valueOf(externalRef); } catch (Exception ignored) {}
                                if (residenciaId != null) {
                                    boolean marked = verifyPreferenceAndMarkResidencia(prefId, residenciaId);
                                    LOGGER.info("{} Marked residencia {} for preference {}: {}", prefixPayment, residenciaId, prefId, marked);
                                }
                            } catch (Exception ex) {
                                LOGGER.warn("{} Error marking residencia for preference {}: {}", prefixPayment, prefId, ex.getMessage());
                            }
                        }

                    } catch (org.springframework.web.client.HttpStatusCodeException hex) {
                        LOGGER.error("{} HTTP error fetching payment {}: status={} body={} message={}", prefixPayment, pid, hex.getStatusCode(), hex.getResponseBodyAsString(), hex.getMessage());
                    } catch (Exception ex) {
                        LOGGER.error("{} Unexpected error fetching payment {}: {}", prefixPayment, pid, ex.getMessage());
                    }
                }
            }

        } catch (org.springframework.web.client.HttpStatusCodeException hex) {
            LOGGER.error("{} HTTP error fetching merchant_order {}: status={} body={} message={}", prefixMO, merchantOrderId, hex.getStatusCode(), hex.getResponseBodyAsString(), hex.getMessage());
        } catch (Exception ex) {
            LOGGER.error("{} Unexpected error processing merchant_order {}: {}", prefixMO, merchantOrderId, ex.getMessage());
        }
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
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, getAccessTokenHeader());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            int status = resp.getStatusCodeValue();
            String body = resp.getBody();
            if (status >= 200 && status < 300) {
                JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
                JsonNode results = root.has("results") ? root.get("results") : null;
                if (results != null && results.isArray()) {
                    for (JsonNode p : results) {
                        String statusField = p.has("status") ? p.get("status").asText("") : "";
                        if ("approved".equalsIgnoreCase(statusField)) return true;
                    }
                }
            } else {
                LOGGER.warn("Preference payments search returned status {} body={}", status, body);
            }
        } catch (HttpStatusCodeException hex) {
            LOGGER.warn("Error searching payments for preference {}: {}", preferenceId, hex.getResponseBodyAsString());
        }
        return false;
    }

    @Override
    public boolean handlePaymentNotification(String paymentId) {
        try {
            if (paymentId == null || paymentId.isBlank()) return false;
            // Get payment details
            String url = "https://api.mercadopago.com/v1/payments/" + paymentId;
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, getAccessTokenHeader());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (resp.getStatusCodeValue() < 200 || resp.getStatusCodeValue() >= 300) {
                LOGGER.warn("Failed to fetch payment {}: status={} body={}", paymentId, resp.getStatusCodeValue(), resp.getBody());
                return false;
            }
            JsonNode paymentJson = objectMapper.readTree(resp.getBody() == null ? "{}" : resp.getBody());
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
            HttpEntity<Void> prefEntity = new HttpEntity<>(headers);
            ResponseEntity<String> prefResp = restTemplate.exchange(prefUrl, HttpMethod.GET, prefEntity, String.class);
            if (prefResp.getStatusCodeValue() < 200 || prefResp.getStatusCodeValue() >= 300) {
                LOGGER.warn("Failed to fetch preference {}: status={} body={}", preferenceId, prefResp.getStatusCodeValue(), prefResp.getBody());
                return false;
            }
            JsonNode prefJson = objectMapper.readTree(prefResp.getBody() == null ? "{}" : prefResp.getBody());
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
