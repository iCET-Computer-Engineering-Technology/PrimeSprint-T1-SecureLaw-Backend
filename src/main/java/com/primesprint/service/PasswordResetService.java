package com.primesprint.service;

import com.primesprint.model.dto.response.ApiResponse;

public interface PasswordResetService {
    ApiResponse sendPasswordResetEmail(String email);

    ApiResponse resetPassword(String reqId, String password);
}
