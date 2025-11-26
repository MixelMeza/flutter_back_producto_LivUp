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
        mailSender.send(message);
    }
}
