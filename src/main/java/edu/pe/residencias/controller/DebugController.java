package edu.pe.residencias.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pe.residencias.service.EmailService;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final EmailService emailService;

    @Value("${app.mail.test.recipient:}")
    private String defaultRecipient;

    @Value("${app.mail.from:no-reply@example.com}")
    private String fromAddress;

    public DebugController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send-test-email")
    public ResponseEntity<?> sendTestEmail(@RequestBody(required = false) Map<String, String> body) {
        String to = null;
        if (body != null) to = body.get("to");
        if (to == null || to.isBlank()) {
            to = defaultRecipient;
        }
        if (to == null || to.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Recipient missing. Provide {\"to\": \"email@domain\"} or set app.mail.test.recipient in application.properties");
        }

        String subject = "Verificación completa: envío de correos funcionando";
        String text = "Este es un correo de verificación enviado manualmente desde el endpoint /api/debug/send-test-email.\n"
                + "Desde: " + fromAddress + "\n"
                + "Fecha: " + edu.pe.residencias.util.DateTimeUtil.nowLima().toString() + "\n\n"
                + "Si recibes este correo significa que la funcionalidad de envío de correos está operativa.";

        try {
            emailService.sendSimpleMessage(to, subject, text);
            return ResponseEntity.ok("Test email sent to " + to);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send test email: " + ex.getMessage());
        }
    }
}
