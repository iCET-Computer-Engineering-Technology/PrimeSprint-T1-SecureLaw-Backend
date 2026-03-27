package com.primesprint.controller;

import com.primesprint.config.JwtProperties;
import com.primesprint.model.dto.UserDto;
import com.primesprint.model.dto.request.LoginRequest;
import com.primesprint.model.dto.request.RegisterRequest;
import com.primesprint.model.dto.response.LoginResponse;
import com.primesprint.model.entity.User;
import com.primesprint.security.JwtUtil;
import com.primesprint.service.AuthService;
import com.primesprint.service.RefreshTokenService;
import com.primesprint.service.impl.RefreshTokenServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenServiceImpl;
    RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request , HttpServletResponse response) {

        User user = authService.authenticate(request);

        // Authenticate user and generate JWT
        String jwt = authService.loginAndGetToken(request);
        Date exp = jwtUtil.extractExpiration(jwt);//
        String refresh = refreshTokenServiceImpl.create(UUID.randomUUID());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(true)
                .path("/api/auth/refresh")
                .maxAge(60 * 60 * 24 * 30)
                .sameSite("Strict")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE,cookie.toString());


        return ResponseEntity.ok(Map.of(
                "token", jwt,
                "expiresAt", exp.getTime(),
                "role",user.getRole().name()));
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
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue("refresh_token") String token,
                                     HttpServletResponse response) {

        Map<String, Object> data = refreshTokenService.verify(token);

        if ((boolean) data.get("revoked")) {
            throw new RuntimeException("Token revoked");
        }

        UUID userId = (UUID) data.get("user_id");

        String newRefresh = refreshTokenService.create(userId);

        ResponseCookie cookie = ResponseCookie.from("refresh_token", newRefresh)
                .httpOnly(true)
                .secure(true)
                .path("/api/auth/refresh")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        User user = authService.getById(userId);

        String access = jwtUtil.generateToken(user);

        return ResponseEntity.ok(Map.of(
                "accessToken", access,
                "accessTokenExpiresAt", jwtUtil.extractExpiration(access).toInstant()
        ));
    }
}