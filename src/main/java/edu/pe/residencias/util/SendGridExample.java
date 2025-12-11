package edu.pe.residencias.util;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Simple standalone example that uses the official SendGrid Java library.
 * Requires the environment variable SENDGRID_API_KEY to be set.
 */
public class SendGridExample {
    public static void main(String[] args) throws IOException {
        String apiKey = System.getenv("SENDGRID_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("SENDGRID_API_KEY not set in environment");
            System.exit(1);
        }

        // Try to load addresses from application.properties on the classpath
        String fromAddr = null;
        String toAddr = null;
        try (InputStream in = SendGridExample.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                fromAddr = p.getProperty("app.mail.from");
                toAddr = p.getProperty("app.mail.test.recipient");
            }
        } catch (Exception e) {
            // ignore and fall back to env/defaults
        }

        // Environment variables override properties file
        String envFrom = System.getenv("APP_MAIL_FROM");
        String envTo = System.getenv("APP_MAIL_TEST_RECIPIENT");
        if (envFrom != null && !envFrom.isBlank()) fromAddr = envFrom;
        if (envTo != null && !envTo.isBlank()) toAddr = envTo;

        if (fromAddr == null || fromAddr.isBlank()) fromAddr = "test@example.com";
        if (toAddr == null || toAddr.isBlank()) toAddr = fromAddr; // default to same address if no test recipient set

        Email from = new Email(fromAddr);
        String subject = "Sending with SendGrid is Fun";
        Email to = new Email(toAddr);
        Content content = new Content("text/plain", "and easy to do anywhere, even with Java");
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(apiKey);
        // sg.setDataResidency("eu"); // uncomment if using EU data residency
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());
            System.out.println("Headers: " + response.getHeaders());
        } catch (IOException ex) {
            throw ex;
        }
    }
}
