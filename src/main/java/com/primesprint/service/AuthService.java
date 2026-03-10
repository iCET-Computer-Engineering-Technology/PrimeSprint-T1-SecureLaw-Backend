package com.primesprint.service;

import com.primesprint.model.entity.User;
import com.primesprint.model.dto.LoginRequest;
import com.primesprint.model.dto.LoginResponse;
import com.primesprint.model.dto.RegisterRequest;
import com.primesprint.repository.RoleRepository;
import com.primesprint.repository.UserRepository;
import com.primesprint.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByUsernameOrEmail(request.getUsernameOrEmail());

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid credentials"
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid credentials"
            );
        }

        String token = jwtUtil.generateToken(user);

        Instant expiresAt = jwtUtil.extractExpiration(token).toInstant();

        return new LoginResponse(
                token,
                expiresAt,
                user.getRole().name()
        );
    }

    public void register(RegisterRequest request) {

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        UUID roleId = roleRepository.findRoleIdByName(request.getRole());
        userRepository.saveUser(
                request.getUsername(),
                request.getEmail(),
                hashedPassword,
                roleId
        );
    }
    public String loginAndGetToken(LoginRequest request) {
        // Authenticate user using login method
        LoginResponse loginResponse = login(request);

        // Return the JWT token
        return loginResponse.getToken();
    }

    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    public User getUserFromToken(String token) {
        String username = jwtUtil.extractUsername(token);
        User user = userRepository.findByUsernameOrEmail(username);

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User not found"
            );
        }
        return user;
    }
}