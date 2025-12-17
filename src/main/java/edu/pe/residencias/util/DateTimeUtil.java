package edu.pe.residencias.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Utility for consistent timestamps using Peru (Lima) timezone.
 */
public final class DateTimeUtil {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private DateTimeUtil() {}

    public static LocalDateTime nowLima() {
        return LocalDateTime.now(LIMA);
    }
}
