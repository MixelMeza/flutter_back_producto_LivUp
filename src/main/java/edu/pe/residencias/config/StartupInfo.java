package edu.pe.residencias.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

@Component
public class StartupInfo implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger logger = LoggerFactory.getLogger(StartupInfo.class);

    @Autowired
    private Environment env;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        try {
            String url = env.getProperty("spring.datasource.url");
            String ddl = env.getProperty("spring.jpa.hibernate.ddl-auto");
            if (url != null) {
                // redact password if present in JDBC URL
                String redacted = url.replaceAll(":\\/\\/([^:]+):([^@]+)@", "://$1:****@");
                logger.info("Datasource URL: {}", redacted);
            } else {
                logger.info("Datasource URL not configured via properties (may be set via environment variables)");
            }
            logger.info("spring.jpa.hibernate.ddl-auto={}", ddl == null ? "(not set)" : ddl);
        } catch (Exception ex) {
            logger.warn("Failed to read startup datasource info", ex);
        }
    }
}
