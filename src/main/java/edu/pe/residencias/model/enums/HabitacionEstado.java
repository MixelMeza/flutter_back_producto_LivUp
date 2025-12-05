package edu.pe.residencias.model.enums;

public enum HabitacionEstado {
    DISPONIBLE("Disponible"),
    RESERVADO("Reservado"),
    OCUPADO("Ocupado"),
    NO_DISPONIBLE("No disponible");

    private final String label;

    HabitacionEstado(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static HabitacionEstado fromString(String s) {
        if (s == null) return null;
        String norm = s.trim().toLowerCase().replace(' ', '_');
        switch (norm) {
            case "disponible": return DISPONIBLE;
            case "reservado": return RESERVADO;
            case "ocupado": return OCUPADO;
            case "no_disponible":
            case "no-disponible":
            case "no disponible": return NO_DISPONIBLE;
            default:
                // try matching enum name
                try {
                    return HabitacionEstado.valueOf(s.toUpperCase());
                } catch (Exception e) {
                    return null;
                }
        }
    }
}
