package edu.pe.residencias.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pe.residencias.util.DateTimeUtil;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @GetMapping("/keepalive")
    public ResponseEntity<?> keepalive() {
        System.out.println("[PublicController] keepalive ping received");
        return ResponseEntity.ok(Map.of("status", "ok", "time", DateTimeUtil.nowLima()));
    }
}
