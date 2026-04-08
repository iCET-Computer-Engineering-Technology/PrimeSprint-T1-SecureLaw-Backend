package com.primesprint.service.impl;

import com.primesprint.model.dto.response.ApiResponse;
import com.primesprint.service.EmailService;
import com.primesprint.service.ResetPasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ResetPasswordServiceImpl implements ResetPasswordService {

    final EmailService emailService;

    @Override
    public ApiResponse reset(String email) {
        return null;
    }

}
