package edu.pe.residencias.exception;

public class DuplicateRegistrationException extends RuntimeException {
    private final String reason;

    public DuplicateRegistrationException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
