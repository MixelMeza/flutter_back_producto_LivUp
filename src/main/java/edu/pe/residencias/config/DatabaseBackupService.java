package edu.pe.residencias.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Component
public class DatabaseBackupService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupService.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment env;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DatabaseBackupService() {
        // support Java 8 date/time types (LocalDate, LocalDateTime, etc.)
        objectMapper.registerModule(new JavaTimeModule());
        // write dates as ISO strings rather than timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void backupNow(String tag) {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String folderName = ts + (tag != null && !tag.isBlank() ? "_" + tag : "");
        Path base = Paths.get("backups", folderName);
        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            logger.error("Could not create backup directory {}", base, e);
            return;
        }

        String dbName = null;
        try (Connection c = dataSource.getConnection()) {
            dbName = c.getCatalog();
        } catch (SQLException e) {
            logger.warn("Could not determine database catalog from DataSource, attempting to parse from URL", e);
            String url = env.getProperty("spring.datasource.url");
            if (url != null) {
                // naive parse: jdbc:mysql://.../dbname[?params]
                int slash = url.lastIndexOf('/');
                if (slash > 0 && slash + 1 < url.length()) {
                    String rest = url.substring(slash + 1);
                    int q = rest.indexOf('?');
                    dbName = q > 0 ? rest.substring(0, q) : rest;
                }
            }
        }

        if (dbName == null || dbName.isBlank()) {
            logger.error("Could not determine database name for backup; aborting");
            return;
        }

        logger.info("Starting DB backup for schema '{}' into {}", dbName, base.toAbsolutePath());

        List<String> tables;
        try {
            tables = jdbcTemplate.queryForList(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = ?",
                    String.class, dbName);
        } catch (Exception e) {
            logger.error("Failed to query information_schema.tables for schema {}", dbName, e);
            return;
        }

        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        ZoneId zone = ZoneId.systemDefault();
        for (String table : tables) {
            try {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM `" + table + "`");
                // convert date/time types to ISO strings for readability
                List<Map<String, Object>> outRows = new java.util.ArrayList<>();
                for (Map<String, Object> row : rows) {
                    Map<String, Object> newRow = new HashMap<>();
                    for (Map.Entry<String, Object> e : row.entrySet()) {
                        newRow.put(e.getKey(), formatValue(e.getValue(), isoFormatter, zone));
                    }
                    outRows.add(newRow);
                }

                Path out = base.resolve(table + ".json");
                try {
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(out.toFile(), outRows);
                } catch (IOException ioe) {
                    logger.error("Failed to write backup file for table {} to {}", table, out, ioe);
                }
            } catch (Exception ex) {
                logger.warn("Skipping table {} due to error reading data", table, ex);
            }
        }

        logger.info("Database backup completed: {}", base.toAbsolutePath());
    }

    private Object formatValue(Object value, DateTimeFormatter isoFormatter, ZoneId zone) {
        if (value == null) return null;
        try {
            if (value instanceof Timestamp) {
                InstantHolder ih = new InstantHolder(((Timestamp) value).toInstant());
                return ih.toIsoString(isoFormatter, zone);
            }
            if (value instanceof java.time.LocalDateTime) {
                LocalDateTime ldt = (LocalDateTime) value;
                return ldt.atZone(zone).format(isoFormatter);
            }
            if (value instanceof Date) {
                InstantHolder ih = new InstantHolder(((Date) value).toInstant());
                return ih.toIsoString(isoFormatter, zone);
            }
            return value;
        } catch (Exception ex) {
            // fallback to original value if formatting fails
            return value;
        }
    }

    // small helper to format Instants consistently (keeps code Java 8 compatible)
    private static class InstantHolder {
        private final java.time.Instant instant;
        InstantHolder(java.time.Instant instant) { this.instant = instant; }
        String toIsoString(DateTimeFormatter fmt, ZoneId zone) {
            return java.time.ZonedDateTime.ofInstant(instant, zone).format(fmt);
        }
    }
}
