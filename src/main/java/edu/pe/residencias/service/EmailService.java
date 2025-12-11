package edu.pe.residencias.service;

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

    public void sendSimpleMessage(String to, String subject, String text) {
        if (mailSender == null) {
            // Mail not configured; log to console for dev
            System.out.println("[EmailService] MailSender not configured. Would send to=" + to + ", subject=" + subject + ", text=" + text);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        // set From using configured property (helps with Gmail/SendGrid providers)
        message.setFrom(fromAddress);
        message.setSubject(subject);
        message.setText(text);

        // Retry logic: try up to 3 times with backoff (5s, 10s)
        int attempts = 0;
        int maxAttempts = 3;
        long[] backoffMs = new long[] {5000L, 10000L};
        while (true) {
            try {
                attempts++;
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
}
