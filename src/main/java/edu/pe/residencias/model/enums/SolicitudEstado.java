package edu.pe.residencias.model.enums;

public enum SolicitudEstado {
    PENDIENTE("pendiente"),
    RESERVADA("reservada"),
    ACEPTADA("aceptada"),
    RECHAZADA("rechazada"),
    CANCELADA("cancelada"),
    VENCIDO("vencido");

    private final String valor;

    SolicitudEstado(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static SolicitudEstado fromValor(String valor) {
        if (valor == null) return null;
        for (SolicitudEstado estado : SolicitudEstado.values()) {
            if (estado.valor.equalsIgnoreCase(valor)) {
                return estado;
            }
        }
        return null;
    }
}
