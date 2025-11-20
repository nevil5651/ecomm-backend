package com.ecommerce.module.auth.service;

import jakarta.mail.MessagingException;

public interface EmailService {
    void sendVerificationEmail(String to, String verificationLink) throws MessagingException;

    void sendPasswordResetEmail(String to, String resetLink) throws MessagingException;
}