package com.taxi.user.controller;

import com.taxi.user.dto.AuthRequest;
import com.taxi.user.dto.AuthResponse;
import com.taxi.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        log.info("POST /auth/login - Login attempt for: {}", request.getEmail());
        AuthResponse response = authService.authenticate(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/passenger")
    public ResponseEntity<AuthResponse> registerPassenger(@RequestBody AuthRequest request) {
        log.info("POST /auth/register/passenger - Registering: {}", request.getEmail());
        AuthResponse response = authService.registerPassenger(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/driver")
    public ResponseEntity<AuthResponse> registerDriver(@RequestBody AuthRequest request) {
        log.info("POST /auth/register/driver - Registering: {}", request.getEmail());
        AuthResponse response = authService.registerDriver(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }
}