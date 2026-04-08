package com.primesprint.service;

import com.primesprint.model.dto.response.ApiResponse;

public interface ResetPasswordService {
    ApiResponse reset(String email);
}
