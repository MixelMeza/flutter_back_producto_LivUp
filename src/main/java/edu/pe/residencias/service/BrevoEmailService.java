package edu.pe.residencias.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BrevoEmailService {

    private final Logger log = LoggerFactory.getLogger(BrevoEmailService.class);

    @Value("${brevo.api.url:https://api.brevo.com/v3/smtp/email}")
    private String brevoApiUrl;

    @Value("${brevo.api.key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Value("${app.mail.from:no-reply@example.com}")
    private String fromAddress;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    /**
     * Send HTML email using Brevo (Sendinblue) v3 SMTP API.
     * @param to recipient email
     * @param subject email subject
     * @param htmlContent html content
     * @throws Exception on failure with details
     */
    public void sendEmail(String to, String subject, String htmlContent) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Brevo API key not configured (set brevo.api.key or BREVO_API_KEY)");
        }
        if (to == null || to.isBlank()) throw new IllegalArgumentException("'to' is required");

        String payload = buildPayload(to.trim(), subject == null ? "" : subject, htmlContent == null ? "" : htmlContent);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(brevoApiUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("api-key", brevoApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        int status = resp.statusCode();
        String body = resp.body();
        if (status < 200 || status >= 300) {
            log.error("Brevo API returned non-2xx: status={} body={}", status, body);
            throw new IllegalStateException("Brevo API returned status=" + status + " body=" + body);
        }
        log.info("Brevo send OK to={} status={}", to, status);
    }

    public boolean isConfigured() {
        return brevoApiKey != null && !brevoApiKey.isBlank();
    }

    private String buildPayload(String to, String subject, String htmlContent) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"sender\":{\"email\":\"").append(escapeJson(fromAddress)).append("\"}");
        sb.append(",\"to\":[{\"email\":\"").append(escapeJson(to)).append("\"}]");
        sb.append(",\"subject\":\"").append(escapeJson(subject)).append("\"");
        sb.append(",\"htmlContent\":\"").append(escapeJson(htmlContent)).append("\"");
        sb.append('}');
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
