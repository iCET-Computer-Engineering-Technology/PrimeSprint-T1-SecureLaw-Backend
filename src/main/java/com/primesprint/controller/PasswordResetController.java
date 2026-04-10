package com.primesprint.controller;

import com.primesprint.model.dto.response.ApiResponse;
import com.primesprint.service.EmailService;
import com.primesprint.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/send-password-reset-email")
    private ApiResponse sendPasswordResetEmail(@RequestParam String email) {
        return passwordResetService.sendPasswordResetEmail(email);
    }

    @PostMapping("/reset-password")
    private ApiResponse resetPassword(@RequestParam String reqId, @RequestParam String password){
        return passwordResetService.resetPassword(reqId, password);
    }


}
