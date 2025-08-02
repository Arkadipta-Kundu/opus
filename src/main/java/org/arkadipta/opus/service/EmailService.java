package org.arkadipta.opus.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    // optionally inject from properties
    @Value("${spring.mail.username}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String to, String otp) {
        String subject = "Your Verification Code";
        String body = "<p>Hi,</p>" +
                "<p>Your verification code is: <b>" + otp + "</b></p>" +
                "<p>This code will expire in 10 minutes.</p>";

        sendHtmlEmail(to, subject, body);
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML
            mailSender.send(message);
        } catch (MessagingException e) {
            // Log and handle appropriately in real app
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
