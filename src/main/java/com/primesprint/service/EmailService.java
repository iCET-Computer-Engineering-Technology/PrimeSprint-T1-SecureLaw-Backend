package com.primesprint.service;

public interface EmailService {
    void sendResetPasswordEmail(String email);
    void sendInvitationEmail(String toEmail, String username, String createdAt,String password);
}
