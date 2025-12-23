package edu.pe.residencias.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility for consistent timestamps using Peru (Lima) timezone.
 */
public final class DateTimeUtil {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private DateTimeUtil() {}

    public static LocalDateTime nowLima() {
        return ZonedDateTime.now(LIMA).toLocalDateTime();
    }

    /**
     * Return a ZonedDateTime in Lima timezone for cases where zone/offset must be preserved.
     */
    public static ZonedDateTime nowLimaZoned() {
        return ZonedDateTime.now(LIMA);
    }

    /**
     * Format a Lima ZonedDateTime into a filesystem-friendly filename fragment.
     * Example: 2025-12-23_15-04-30
     */
    public static String formatForFilename(ZonedDateTime zdt) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        return zdt.format(f);
    }
}
