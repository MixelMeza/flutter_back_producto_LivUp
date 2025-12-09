package edu.pe.residencias.model.enums;

public enum ContratoEstado {
    PENDIENTE_INICIO("pendiente_inicio"),
    VIGENTE("vigente"),
    VENCIDO("vencido"),
    CANCELADO("cancelado"),
    RENOVADO("renovado"),
    FINALIZADO("finalizado");

    private final String valor;

    ContratoEstado(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static ContratoEstado fromValor(String valor) {
        if (valor == null) return null;
        for (ContratoEstado estado : ContratoEstado.values()) {
            if (estado.valor.equalsIgnoreCase(valor)) {
                return estado;
            }
        }
        return null;
    }
}
