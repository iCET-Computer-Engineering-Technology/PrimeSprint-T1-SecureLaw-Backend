package com.primesprint.service;

public interface EmailService {
    void sendInvitationEmail(String toEmail, String username, String createdAt,String password);
    void sendPasswordResetEmail(String toEmail, String token);
}
