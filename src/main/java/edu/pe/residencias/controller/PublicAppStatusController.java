package edu.pe.residencias.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pe.residencias.service.AppStatusService;

@RestController
@RequestMapping("/api/public/app-status")
public class PublicAppStatusController {

    @Autowired
    private AppStatusService appStatusService;

    @GetMapping("")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(appStatusService.getStatus());
    }
}
