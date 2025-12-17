package edu.pe.residencias.controller.auth;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
    // Optional device info sent by frontend
    private String fcmToken;
    private String plataforma;
    private String modelo;
    private String osVersion;
}
