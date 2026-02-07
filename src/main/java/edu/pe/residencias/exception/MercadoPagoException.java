package edu.pe.residencias.exception;

public class MercadoPagoException extends RuntimeException {
    public MercadoPagoException(String message) {
        super(message);
    }

    public MercadoPagoException(String message, Throwable cause) {
        super(message, cause);
    }
}
