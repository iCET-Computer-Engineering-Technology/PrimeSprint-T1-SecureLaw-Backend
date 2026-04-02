package com.primesprint.controller;

import com.primesprint.model.dto.UserDto;
import com.primesprint.model.dto.request.LoginRequest;
import com.primesprint.model.dto.request.RegisterRequest;
import com.primesprint.model.entity.User;
import com.primesprint.security.JwtUtil;
import com.primesprint.service.AuthService;
import com.primesprint.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpServletRequest servletRequest,
                                   HttpServletResponse response) {

        User user = authService.authenticate(request);

        String accessToken = jwtUtil.generateToken(user);
        Date exp = jwtUtil.extractExpiration(accessToken);
        String refresh = refreshTokenService.create(user.getId());

        response.addHeader(HttpHeaders.SET_COOKIE,
                buildRefreshCookie(refresh, Duration.ofDays(30), servletRequest.isSecure()).toString());

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "accessTokenExpiresAt", exp.toInstant(),
                "role", user.getRole().name()));
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        if (userDetails == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Unauthorized"
            );
        }

        UserDto user = authService.getUserByUsername(
                userDetails.getUsername()
        );

        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "refresh_token", required = false) String token,
                                    HttpServletRequest servletRequest,
                                    HttpServletResponse response) {

        if (token != null && !token.isBlank()) {
            refreshTokenService.revoke(token);
        }

        response.addHeader(HttpHeaders.SET_COOKIE,
                buildRefreshCookie("", Duration.ZERO, servletRequest.isSecure()).toString());

        return ResponseEntity.ok("Logged out");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(value = "refresh_token", required = false) String token,
                                     HttpServletRequest servletRequest,
                                     HttpServletResponse response) {

        Map<String, Object> data = refreshTokenService.verify(token);

        UUID userId = UUID.fromString(data.get("user_id").toString());

        String newRefresh = refreshTokenService.create(userId);
        refreshTokenService.revoke(token);

        response.addHeader(HttpHeaders.SET_COOKIE,
                buildRefreshCookie(newRefresh, Duration.ofDays(30), servletRequest.isSecure()).toString());

        User user = authService.getById(userId);

        String access = jwtUtil.generateToken(user);

        return ResponseEntity.ok(Map.of(
                "accessToken", access,
                "accessTokenExpiresAt", jwtUtil.extractExpiration(access).toInstant()
        ));
    }

    private ResponseCookie buildRefreshCookie(String value, Duration maxAge, boolean secure) {
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(secure)
                .path("/api/auth")
                .sameSite(secure ? "None" : "Lax")
                .maxAge(maxAge)
                .build();
    }
}