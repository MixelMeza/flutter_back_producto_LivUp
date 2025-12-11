package edu.pe.residencias.startup;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import edu.pe.residencias.service.EmailService;

@Component
public class EmailStartupVerifier {

    private final EmailService emailService;

    @Value("${app.mail.test.recipient:}")
    private String testRecipient;

    @Value("${app.mail.from:no-reply@example.com}")
    private String fromAddress;

    public EmailStartupVerifier(EmailService emailService) {
        this.emailService = emailService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // If no test recipient configured, skip (avoid spamming)
        if (testRecipient == null || testRecipient.isBlank()) {
            System.out.println("[EmailStartupVerifier] No test recipient configured (app.mail.test.recipient). Skipping startup email.");
            return;
        }

        String subject = "Verificación completa: envío de correos funcionando";
        String body = "Este es un correo de verificación enviado automáticamente al iniciar la aplicación.\n"
                + "Aplicación: " + System.getProperty("spring.application.name", "residencias-backend") + "\n"
                + "Desde: " + fromAddress + "\n"
                + "Fecha: " + LocalDateTime.now().toString() + "\n\n"
                + "Si recibes este correo significa que la funcionalidad de envío de correos está operativa.";

        try {
            System.out.println("[EmailStartupVerifier] Enviando correo de prueba a: " + testRecipient);
            emailService.sendSimpleMessage(testRecipient, subject, body);
            System.out.println("[EmailStartupVerifier] Envío de correo de prueba completado (ver logs/Inbox del destinatario).");
        } catch (Exception ex) {
            System.err.println("[EmailStartupVerifier] Error al enviar correo de prueba: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
