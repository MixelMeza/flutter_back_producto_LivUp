package edu.pe.residencias.model.enums;

public enum PagoEstado {
    PAGADO("pagado"),
    PENDIENTE("pendiente"),
    VENCIDO("vencido"),
    COMPLETADO("completado"),
    PAID("paid");

    private final String valor;

    PagoEstado(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static PagoEstado fromValor(String valor) {
        if (valor == null) return null;
        for (PagoEstado estado : PagoEstado.values()) {
            if (estado.valor.equalsIgnoreCase(valor)) {
                return estado;
            }
        }
        return null;
    }
}
