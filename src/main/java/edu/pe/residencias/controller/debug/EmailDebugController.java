package edu.pe.residencias.controller.debug;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import edu.pe.residencias.service.BrevoEmailService;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/debug")
public class EmailDebugController {

    @Autowired
    private BrevoEmailService brevoEmailService;

    public static class SendRequest {
        @NotBlank @Email
        public String to;
        @NotBlank
        public String subject;
        @NotBlank
        public String html;
    }

    @PostMapping("/send-brevo")
    public ResponseEntity<?> sendBrevo(@RequestBody SendRequest req) {
        try {
            if (!brevoEmailService.isConfigured()) {
                return ResponseEntity.status(503).body("Brevo API key not configured");
            }
            brevoEmailService.sendEmail(req.to, req.subject, req.html);
            return ResponseEntity.accepted().body("Sent");
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Error sending via Brevo: " + ex.getMessage());
        }
    }
}
