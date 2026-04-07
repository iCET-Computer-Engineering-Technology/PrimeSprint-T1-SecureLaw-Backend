package com.primesprint.service;

public interface EmailService {
    void sendInvitationEmail(String toEmail, String accessLink, String username, String temporaryPassword, String createdAt);
}
