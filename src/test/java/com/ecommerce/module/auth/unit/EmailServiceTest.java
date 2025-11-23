package com.ecommerce.module.auth.unit;

import com.ecommerce.module.auth.service.impl.SpringEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.thymeleaf.spring6.SpringTemplateEngine;

import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.internet.MimeMessage;

import static org.mockito.Mockito.*;

class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine thymeleaf;

    @InjectMocks
    private SpringEmailService emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sendVerificationEmail_invokesMailSender() throws Exception {
        MimeMessage mime = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mime);
        when(thymeleaf.process(anyString(), any())).thenReturn("<p>ok</p>");

        emailService.sendVerificationEmail("test@e.com", "https://app/verify?token=abc");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
