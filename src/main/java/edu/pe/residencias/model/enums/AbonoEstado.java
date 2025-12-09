package edu.pe.residencias.model.enums;

public enum AbonoEstado {
    REGISTRADO("registrado"),
    ANULADO("anulado");

    private final String valor;

    AbonoEstado(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static AbonoEstado fromValor(String valor) {
        if (valor == null) return null;
        for (AbonoEstado estado : AbonoEstado.values()) {
            if (estado.valor.equalsIgnoreCase(valor)) {
                return estado;
            }
        }
        return null;
    }
}
