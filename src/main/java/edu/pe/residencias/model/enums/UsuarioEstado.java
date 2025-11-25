package edu.pe.residencias.model.enums;

public enum UsuarioEstado {
    ACTIVO("activo"),
    INACTIVO("inactivo"),
    SUSPENDIDO("suspendido");

    private final String valor;

    UsuarioEstado(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static UsuarioEstado fromValor(String valor) {
        if (valor == null) return null;
        for (UsuarioEstado estado : UsuarioEstado.values()) {
            if (estado.valor.equalsIgnoreCase(valor)) {
                return estado;
            }
        }
        return null;
    }
}
