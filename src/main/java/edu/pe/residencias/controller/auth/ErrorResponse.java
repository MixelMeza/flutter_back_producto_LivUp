package edu.pe.residencias.controller.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private String reason;
    private String message;

    public ErrorResponse(String message) {
        this.reason = null;
        this.message = message;
    }
}
