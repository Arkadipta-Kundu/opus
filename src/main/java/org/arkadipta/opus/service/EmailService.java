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
        String body = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #f6f6f6; margin: 0; padding: 0; }" +
                ".container { background: #fff; max-width: 500px; margin: 40px auto; padding: 30px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }"
                +
                ".header { font-size: 22px; color: #333; margin-bottom: 20px; }" +
                ".otp { font-size: 28px; color: #007bff; font-weight: bold; letter-spacing: 2px; margin: 20px 0; }" +
                ".footer { font-size: 13px; color: #888; margin-top: 30px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>Verification Code</div>" +
                "<p>Dear User,</p>" +
                "<p>Please use the following verification code to complete your action:</p>" +
                "<div class='otp'>" + otp + "</div>" +
                "<p>This code will expire in <b>10 minutes</b>. If you did not request this, please ignore this email.</p>"
                +
                "<div class='footer'>Thank you,<br/>Opus Team</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        sendHtmlEmail(to, subject, body);
    }

    public void sendResetLinkEmail(String email, String resetLink) {
        String subject = "Password Reset Request";
        String message = "Click the link below to reset your password:\n" + resetLink;

        sendHtmlEmail(email, subject, message);
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
            // Log and handle appropriately in a real app
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
