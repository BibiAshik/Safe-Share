package com.safeshare.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Async
    public void sendVerificationEmail(String to, String name, String verificationLink) {
        String body = "Hi " + name + ",\n\n"
                + "Click here to verify your SafeShare account:\n"
                + verificationLink + "\n\n"
                + "This link expires in 1 hour.\n\n"
                + "SafeShare";
        send(to, "Verify your SafeShare account", body);
    }

    @Async
    public void sendPasswordResetEmail(String to, String name, String resetLink) {
        String body = "Hi " + name + ",\n\n"
                + "Click here to reset your SafeShare password:\n"
                + resetLink + "\n\n"
                + "This link expires in 1 hour. If you did not request this, you can ignore this email.\n\n"
                + "SafeShare";
        send(to, "Reset your SafeShare password", body);
    }

    private void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
