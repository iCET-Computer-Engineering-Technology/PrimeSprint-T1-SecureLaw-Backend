package com.primesprint.controller;

import com.primesprint.model.dto.LoginRequest;
import com.primesprint.model.dto.LoginResponse;
import com.primesprint.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {

        String token = authService.login(
                request.getUsernameOrEmail(),
                request.getPassword()
        );

        return ResponseEntity.ok(new LoginResponse(token));
    }
}