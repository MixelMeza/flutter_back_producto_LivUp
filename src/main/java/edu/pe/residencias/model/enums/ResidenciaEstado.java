package edu.pe.residencias.model.enums;

public enum ResidenciaEstado {
    ACTIVO("Activo"),
    ELIMINADO("Eliminado"),
    OCULTO("Oculto"),
    INACTIVO("Inactivo"),
    SUSPENDIDO("Suspendido");

    private final String value;

    ResidenciaEstado(String value) { this.value = value; }

    @Override
    public String toString() { return this.value; }

    public static boolean isValid(String s) {
        if (s == null) return false;
        for (ResidenciaEstado e : values()) {
            if (e.value.equalsIgnoreCase(s) || e.name().equalsIgnoreCase(s)) return true;
        }
        return false;
    }
}
