package com.bytebites.authservice.controller;

import com.bytebites.authservice.dto.AuthRequest;
import com.bytebites.authservice.dto.AuthResponse;
import com.bytebites.authservice.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody AuthRequest authRequest) {
        try {
            String response = authService.registerUser(authRequest.getEmail(), authRequest.getPassword());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginAndGenerateToken(@RequestBody AuthRequest authRequest) {
            AuthResponse authResponse = authService.authenticateAndGenerateToken(authRequest.getEmail(), authRequest.getPassword());
            return ResponseEntity.ok(authResponse);
    }
}