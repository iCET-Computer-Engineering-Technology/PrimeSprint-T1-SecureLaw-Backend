package com.primesprint.controller;

import com.primesprint.model.dto.UserDto;
import com.primesprint.model.dto.request.LoginRequest;
import com.primesprint.model.dto.request.RegisterRequest;
import com.primesprint.model.dto.response.LoginResponse;
import com.primesprint.service.impl.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // Authenticate user and generate JWT
        String jwt = authService.loginAndGetToken(request);
        // Build LoginResponse with all required fields; client must store token and send it as Authorization: Bearer <token>
        UserDto user = authService.getUserFromToken(jwt);
        LoginResponse loginResponse = new LoginResponse(
                jwt,
                Instant.now(),
                user.getRole().name()
        );

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal UserDetails userDetails) {
        // User is resolved from SecurityContext that JwtFilter populated using Authorization: Bearer <token>
        UserDto user = authService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // Stateless JWT: logout is handled client-side by discarding the token.
        return ResponseEntity.ok("Logged out successfully; please remove token on client side.");
    }


    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody UserDto userDto) {
       authService.resetPassword(userDto.getEmail(), userDto.getResetPassword());
        return ResponseEntity.ok("Password has been reset successfully");
    }
}
