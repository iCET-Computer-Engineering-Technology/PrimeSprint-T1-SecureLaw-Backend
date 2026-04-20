package com.primesprint.service.impl;

import com.primesprint.model.dto.response.ApiResponse;
import com.primesprint.model.entity.User;
import com.primesprint.repository.PasswordResetRepository;
import com.primesprint.repository.UserRepository;
import com.primesprint.service.EmailService;
import com.primesprint.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetRepository passwordResetRepository;
    public final UserRepository userRepository;

    @Override
    public ApiResponse sendPasswordResetEmail(String email) {
        User user = userRepository.findByUsernameOrEmail(email);

        if (user==null){
            throw new IllegalStateException("User not found for this email : "+email);
        }

        emailService.sendPasswordResetEmail(email,generatePasswordResetToken(user));

        return ResponseEntity.ok(
                ApiResponse.success(200, "Password reset email sent successfully", null)
        ).getBody();
    }

    @Override
    public ApiResponse resetPassword(String reqId, String password) {
        boolean isAvailable = passwordResetRepository.isAvailable(reqId);

        if (!isAvailable) {
            throw new IllegalStateException("Invalid password reset token");
        }

        boolean updated = passwordResetRepository.resetPassword(reqId, passwordEncoder.encode(password));
        if (!updated) {
            throw new IllegalStateException("Invalid or expired password reset token");
        }

        return ResponseEntity.ok(
                ApiResponse.success(200, "Password reset successfully", null)
        ).getBody();
    }

    private String generatePasswordResetToken(User user){
        String id = UUID.randomUUID().toString();
        passwordResetRepository.savePasswordResetToken(user.getId(), id);
        return id;
    }
}
