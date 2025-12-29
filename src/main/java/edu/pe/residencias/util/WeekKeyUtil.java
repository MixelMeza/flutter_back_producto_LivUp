package edu.pe.residencias.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * ISO week key helper.
 * Format: yyyy-'W'ww (e.g. 2025-W06)
 * Uses America/Lima timezone.
 */
public final class WeekKeyUtil {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private WeekKeyUtil() {}

    public static String currentWeekKeyLima() {
        ZonedDateTime zdt = ZonedDateTime.now(LIMA);
        WeekFields wf = WeekFields.ISO;
        int weekBasedYear = zdt.get(wf.weekBasedYear());
        int week = zdt.get(wf.weekOfWeekBasedYear());
        return String.format(Locale.ROOT, "%d-W%02d", weekBasedYear, week);
    }
}
