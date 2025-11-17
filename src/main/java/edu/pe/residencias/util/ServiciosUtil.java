package edu.pe.residencias.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ServiciosUtil {

    /**
     * Normalize servicios text: trim, remove empty, join by comma.
     * Accepts separators: comma, semicolon, newline.
     */
    public static String normalizeServiciosText(String raw) {
        if (raw == null) return null;
        List<String> parts = Arrays.stream(raw.split("[,;\\r?\\n]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return String.join(",", parts);
    }

    /**
     * Parse servicios text into simple list of words (keys)
     */
    public static List<String> parseServicios(String text) {
        if (text == null) return java.util.Collections.emptyList();
        return Arrays.stream(text.split("[,;\\r?\\n]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
