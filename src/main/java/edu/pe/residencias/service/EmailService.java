package edu.pe.residencias.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@example.com}")
    private String fromAddress;

    @Value("${app.mail.reply-to:}")
    private String replyToAddress;

    @Value("${SENDGRID_API_KEY:}")
    private String sendgridApiKey;

    public void sendSimpleMessage(String to, String subject, String text) {
        // If SendGrid API key is configured, prefer HTTP API (avoids SMTP egress blocks)
        if (sendgridApiKey != null && !sendgridApiKey.isBlank()) {
            try {
                sendViaSendGrid(to, subject, text);
                return;
            } catch (Exception ex) {
                System.err.println("[EmailService] SendGrid API send failed, falling back to JavaMailSender: " + ex.getMessage());
                // continue to SMTP fallback
            }
        }

        if (mailSender == null) {
            // Mail not configured; log to console for dev
            System.out.println("[EmailService] MailSender not configured. Would send to=" + to + ", subject=" + subject + ", text=" + text);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        // set From using configured property (helps with Gmail/SendGrid providers)
        message.setFrom(fromAddress);
        if (replyToAddress != null && !replyToAddress.isBlank()) {
            message.setReplyTo(replyToAddress);
        }
        message.setSubject(subject);
        message.setText(text);

        // Retry logic: try up to 3 times with backoff (5s, 10s)
        int attempts = 0;
        int maxAttempts = 3;
        long[] backoffMs = new long[] {5000L, 10000L};
        while (true) {
            try {
                attempts++;
                System.out.println("[EmailService] Sending via SMTP from=" + fromAddress + " replyTo=" + replyToAddress + " to=" + to + " subject=" + subject);
                mailSender.send(message);
                // success
                if (attempts > 1) {
                    System.out.println("[EmailService] Email sent on attempt " + attempts + " to=" + to);
                }
                break;
            } catch (Exception ex) {
                System.err.println("[EmailService] Attempt " + attempts + " failed to send email to=" + to + ": " + ex.getMessage());
                if (attempts >= maxAttempts) {
                    System.err.println("[EmailService] All attempts failed to send email to=" + to);
                    throw ex;
                }
                // sleep before retry
                long sleepMs = backoffMs[Math.min(attempts - 1, backoffMs.length - 1)];
                try { Thread.sleep(sleepMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    private void sendViaSendGrid(String to, String subject, String text) throws Exception {
        if (sendgridApiKey == null || sendgridApiKey.isBlank()) throw new IllegalStateException("SENDGRID_API_KEY not configured");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        StringBuilder b = new StringBuilder();
        b.append("{\"personalizations\":[{\"to\":[{\"email\":\"").append(escapeJson(to)).append("\"}]}]");
        b.append(",\"from\":{\"email\":\"").append(escapeJson(fromAddress)).append("\"}");
        if (replyToAddress != null && !replyToAddress.isBlank()) {
            b.append(",\"reply_to\":{\"email\":\"").append(escapeJson(replyToAddress)).append("\"}");
        }
        b.append(",\"subject\":\"").append(escapeJson(subject)).append("\",\"content\":[{\"type\":\"text/plain\",\"value\":\"").append(escapeJson(text)).append("\"}]}");
        String body = b.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + sendgridApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        String respBody = response.body();
        System.out.println("[EmailService] SendGrid response status=" + status + " body=" + respBody + " headers=" + response.headers().map());
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("SendGrid API returned status=" + status + ", body=" + respBody);
        }
        System.out.println("[EmailService] Sent email via SendGrid to=" + to + " status=" + status);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
