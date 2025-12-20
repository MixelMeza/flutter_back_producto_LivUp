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
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;

// Using HTTP Resend API instead of SDK (SDK not available in current pom)

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
    
    @Value("${RESENDER_API_KEY:}")
    private String resenderApiKey;

    // Support both environment variable names: RESENDER_API_KEY (legacy in this project)
    // and RESEND_API_KEY (official Resend service token like re_xxx)
    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Value("${resender.api.url:https://api.resend.com/emails}")
    private String resenderApiUrl;

    @Value("${app.mail.smtp.max-attempts:1}")
    private int smtpMaxAttempts;

    @Value("${app.mail.smtp.fallback:false}")
    private boolean smtpFallback;

    public void sendSimpleMessage(String to, String subject, String text) {
        // If JavaMailSender (SMTP) is configured, prefer SMTP sends first.
        if (mailSender != null) {
            try {
                sendViaSmtp(to, subject, text);
                return;
            } catch (Exception ex) {
                // Log full stack trace and cause chain for exact reason
                System.err.println("[EmailService] SMTP send failed: " + ex.toString());
                ex.printStackTrace(System.err);
                Throwable c = ex.getCause();
                while (c != null) {
                    System.err.println("[EmailService] Caused by: " + c.toString());
                    c.printStackTrace(System.err);
                    c = c.getCause();
                }
                if (!smtpFallback) {
                    // Do not attempt HTTP fallbacks; rethrow so callers receive the error
                    throw new IllegalStateException("SMTP send failed and smtpFallback is disabled", ex);
                }
                System.err.println("[EmailService] SMTP failed; smtpFallback=true so trying HTTP providers");
            }
        }
        // Prefer Resend HTTP API if API key provided (RESEND_API_KEY or RESENDER_API_KEY)
        String apiKey = (resendApiKey != null && !resendApiKey.isBlank()) ? resendApiKey : resenderApiKey;
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                sendViaResender(to, subject, text);
                return;
            } catch (Exception ex) {
                System.err.println("[EmailService] Resend HTTP send failed, falling back to SendGrid/SMTP: " + ex.getMessage());
                // continue to other fallbacks
            }
        }

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

        // No SMTP configured: log and continue to other providers
        System.out.println("[EmailService] MailSender not configured. Would send to=" + to + ", subject=" + subject + ", text=" + text);
    }

    public void sendHtmlMessage(String to, String subject, String html, String plainText) {
        // Try SMTP with HTML multipart if available
        if (mailSender != null) {
            try {
                sendViaSmtpHtml(to, subject, html, plainText);
                return;
            } catch (Exception ex) {
                System.err.println("[EmailService] SMTP HTML send failed: " + ex.toString());
                ex.printStackTrace(System.err);
                Throwable c = ex.getCause();
                while (c != null) {
                    System.err.println("[EmailService] Caused by: " + c.toString());
                    c.printStackTrace(System.err);
                    c = c.getCause();
                }
                if (!smtpFallback) {
                    throw new IllegalStateException("SMTP HTML send failed and smtpFallback is disabled", ex);
                }
                System.err.println("[EmailService] SMTP HTML failed; smtpFallback=true so trying HTTP providers");
            }
        }

        // Try Resend HTTP API (it accepts html/text)
        String apiKey = (resendApiKey != null && !resendApiKey.isBlank()) ? resendApiKey : resenderApiKey;
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                // reuse sendViaResender; it checks for html content based on presence of tags
                sendViaResender(to, subject, html != null && !html.isBlank() ? html : plainText);
                return;
            } catch (Exception ex) {
                System.err.println("[EmailService] Resend HTTP send failed, falling back to SendGrid/SMTP: " + ex.getMessage());
            }
        }

        // SendGrid fallback
        if (sendgridApiKey != null && !sendgridApiKey.isBlank()) {
            try {
                // SendGrid payload currently only supports text in our helper; for HTML send as text field with html tags
                sendViaSendGrid(to, subject, html != null && !html.isBlank() ? html : plainText);
                return;
            } catch (Exception ex) {
                System.err.println("[EmailService] SendGrid API send failed, falling back to JavaMailSender: " + ex.getMessage());
            }
        }

        System.out.println("[EmailService] MailSender not configured. Would send HTML email to=" + to + ", subject=" + subject);
    }

    private void sendViaSmtpHtml(String to, String subject, String html, String plainText) throws MessagingException {
        if (mailSender == null) throw new IllegalStateException("JavaMailSender not configured");

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
        helper.setTo(to);
        helper.setFrom(fromAddress);
        if (replyToAddress != null && !replyToAddress.isBlank()) {
            helper.setReplyTo(replyToAddress);
        }
        helper.setSubject(subject);
        // set plain text and html alternative
        helper.setText(plainText == null ? "" : plainText, html == null ? "" : html);

        int attempts = 0;
        int maxAttempts = Math.max(1, smtpMaxAttempts);
        long[] backoffMs = new long[] {5000L, 10000L};
        while (true) {
            try {
                attempts++;
                System.out.println("[EmailService] Sending HTML via SMTP from=" + fromAddress + " to=" + to + " subject=" + subject);
                mailSender.send(mimeMessage);
                if (attempts > 1) {
                    System.out.println("[EmailService] Email sent on attempt " + attempts + " to=" + to);
                }
                return;
            } catch (Exception ex) {
                System.err.println("[EmailService] Attempt " + attempts + " failed to send HTML email to=" + to + ": " + ex.getMessage());
                if (attempts >= maxAttempts) {
                    System.err.println("[EmailService] All attempts failed to send HTML email to=" + to);
                    throw ex;
                }
                long sleepMs = backoffMs[Math.min(attempts - 1, backoffMs.length - 1)];
                try { Thread.sleep(sleepMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    private void sendViaSmtp(String to, String subject, String text) {
        if (mailSender == null) throw new IllegalStateException("JavaMailSender not configured");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(fromAddress);
        if (replyToAddress != null && !replyToAddress.isBlank()) {
            message.setReplyTo(replyToAddress);
        }
        message.setSubject(subject);
        message.setText(text);

        int attempts = 0;
        int maxAttempts = 3;
        long[] backoffMs = new long[] {5000L, 10000L};
        while (true) {
            try {
                attempts++;
                System.out.println("[EmailService] Sending via SMTP from=" + fromAddress + " replyTo=" + replyToAddress + " to=" + to + " subject=" + subject);
                mailSender.send(message);
                if (attempts > 1) {
                    System.out.println("[EmailService] Email sent on attempt " + attempts + " to=" + to);
                }
                return;
            } catch (Exception ex) {
                System.err.println("[EmailService] Attempt " + attempts + " failed to send email to=" + to + ": " + ex.getMessage());
                if (attempts >= maxAttempts) {
                    System.err.println("[EmailService] All attempts failed to send email to=" + to);
                    throw ex;
                }
                long sleepMs = backoffMs[Math.min(attempts - 1, backoffMs.length - 1)];
                try { Thread.sleep(sleepMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    // Resend SDK helper removed; using HTTP-based sendViaResender instead.

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

    private void sendViaResender(String to, String subject, String text) throws Exception {
        String apiKey = (resendApiKey != null && !resendApiKey.isBlank()) ? resendApiKey : resenderApiKey;
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("RESEND_API_KEY / RESENDER_API_KEY not configured");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        // Resend API expects `to` as array. Support comma-separated `to` values from callers.
        String[] tos = to == null ? new String[0] : to.split("\\s*,\\s*");

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"from\":\"").append(escapeJson(fromAddress)).append("\"");
        // `to` should be a string for single recipient, or an array of strings for multiple.
        if (tos.length == 1) {
            sb.append(",\"to\":\"").append(escapeJson(tos[0].trim())).append("\"");
        } else {
            sb.append(",\"to\":[");
            for (int i = 0; i < tos.length; i++) {
                if (i > 0) sb.append(',');
                sb.append('"').append(escapeJson(tos[i].trim())).append('"');
            }
            sb.append(']');
        }
        if (replyToAddress != null && !replyToAddress.isBlank()) {
            // Resend API expects reply_to as a string email address
            sb.append(",\"reply_to\":\"").append(escapeJson(replyToAddress)).append("\"");
        }
        sb.append(",\"subject\":\"").append(escapeJson(subject)).append("\"");
        // Prefer HTML when message looks like HTML, otherwise send as text
        if (text != null && (text.contains("<") && text.contains("</"))) {
            sb.append(",\"html\":\"").append(escapeJson(text)).append("\"");
        } else {
            sb.append(",\"text\":\"").append(escapeJson(text)).append("\"");
        }
        sb.append('}');

        String body = sb.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(resenderApiUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        String respBody = response.body();
        System.out.println("[EmailService] Resend response status=" + status + " body=" + respBody + " headers=" + response.headers().map());
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("Resend API returned status=" + status + ", body=" + respBody);
        }
        System.out.println("[EmailService] Sent email via Resend to=" + to + " status=" + status);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
