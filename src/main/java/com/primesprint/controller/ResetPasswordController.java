package com.primesprint.controller;

import com.primesprint.model.dto.response.ApiResponse;
import com.primesprint.service.ResetPasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reset-password")
@RequiredArgsConstructor
public class ResetPasswordController {

    final ResetPasswordService resetPasswordService;

    @GetMapping("/reset")
    public ApiResponse<String> resetPassword(@PathVariable String email) {
        return resetPasswordService.reset(email);
    }
}
