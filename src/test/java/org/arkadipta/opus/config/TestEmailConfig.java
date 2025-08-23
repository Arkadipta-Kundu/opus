package org.arkadipta.opus.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

@TestConfiguration
public class TestEmailConfig {

    @Bean
    @Primary
    public JavaMailSender mockJavaMailSender() {
        return new JavaMailSender() {
            @Override
            public MimeMessage createMimeMessage() {
                return new MimeMessage(Session.getDefaultInstance(new Properties()));
            }

            @Override
            public MimeMessage createMimeMessage(java.io.InputStream contentStream) {
                throw new UnsupportedOperationException("Mock implementation");
            }

            @Override
            public void send(MimeMessage mimeMessage) {
                // Mock implementation - do nothing
                System.out.println("Mock: Sending email...");
            }

            @Override
            public void send(MimeMessage... mimeMessages) {
                // Mock implementation - do nothing
                System.out.println("Mock: Sending emails...");
            }

            @Override
            public void send(MimeMessagePreparator mimeMessagePreparator) {
                // Mock implementation - do nothing
                System.out.println("Mock: Sending email with preparator...");
            }

            @Override
            public void send(MimeMessagePreparator... mimeMessagePreparators) {
                // Mock implementation - do nothing
                System.out.println("Mock: Sending emails with preparators...");
            }

            @Override
            public void send(SimpleMailMessage simpleMessage) {
                // Mock implementation - do nothing
                System.out.println("Mock: Sending simple email to: " + 
                    (simpleMessage.getTo() != null ? String.join(", ", simpleMessage.getTo()) : "unknown"));
            }

            @Override
            public void send(SimpleMailMessage... simpleMessages) {
                // Mock implementation - do nothing
                System.out.println("Mock: Sending " + simpleMessages.length + " simple emails...");
            }
        };
    }
}
