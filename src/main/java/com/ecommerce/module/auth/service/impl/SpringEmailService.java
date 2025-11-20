package com.ecommerce.module.auth.service.impl;

import com.ecommerce.module.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpringEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine thymeleaf;

    @Override
    public void sendVerificationEmail(String to, String verificationLink) throws MessagingException {
        Context ctx = new Context();
        ctx.setVariable("verificationLink", verificationLink);
        ctx.setVariable("recipient", to);
        String html = thymeleaf.process("verify-email.html", ctx);
        sendHtml(to, "Verify your email", html);
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) throws MessagingException {
        Context ctx = new Context();
        ctx.setVariable("resetLink", resetLink);
        ctx.setVariable("recipient", to);
        String html = thymeleaf.process("reset-password.html", ctx);
        sendHtml(to, "Reset your password", html);
    }

    private void sendHtml(String to, String subject, String html) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
        log.info("Sent '{}' to {}", subject, to);
    }
}