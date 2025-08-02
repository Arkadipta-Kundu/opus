# OTP Email Verification Guide

This guide will walk you through the process of implementing OTP email verification in your Spring Boot application. Since you are learning, we will keep the implementation simple and focus on the core concepts.

## Overview

OTP (One-Time Password) email verification is a common feature used to verify a user's email address. The process involves:

1. Generating an OTP.
2. Sending the OTP to the user's email.
3. Verifying the OTP entered by the user.

## Prerequisites

Ensure you have the following dependencies added to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

## Step 1: Configure Email Properties

Add the following properties to your `application.properties` file:

```properties
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=your-email@example.com
spring.mail.password=your-email-password
spring.mail.protocol=smtp
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

Replace `your-email@example.com` and `your-email-password` with your actual email credentials.

## Step 2: Create the EmailService

The `EmailService` is responsible for sending emails. You have already created this service. Here is the code for reference:

```java
@Service
public class EmailService {

    private final JavaMailSender mailSender;

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
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
```

## Step 3: Create the OTP Utility

The OTP utility will generate random OTPs. Here is a simple implementation:

```java
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpUtility {

    private static final int OTP_LENGTH = 6;

    public String generateOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
}
```

## Step 4: Create the Controller

The controller will handle the user request to send and verify the OTP. Here is the code:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private OtpUtility otpUtility;

    private Map<String, String> otpStore = new HashMap<>();

    @PostMapping("/send")
    public String sendOtp(@RequestParam String email) {
        String otp = otpUtility.generateOtp();
        otpStore.put(email, otp);
        emailService.sendOtpEmail(email, otp);
        return "OTP sent to " + email;
    }

    @PostMapping("/verify")
    public String verifyOtp(@RequestParam String email, @RequestParam String otp) {
        String storedOtp = otpStore.get(email);
        if (storedOtp != null && storedOtp.equals(otp)) {
            otpStore.remove(email);
            return "OTP verified successfully!";
        } else {
            return "Invalid OTP or OTP expired.";
        }
    }
}
```

## Step 5: Test the Application

1. Start your Spring Boot application.
2. Use a tool like Postman to test the endpoints:
   - `POST /api/otp/send` with `email` as a parameter to send the OTP.
   - `POST /api/otp/verify` with `email` and `otp` as parameters to verify the OTP.

## How It Works

1. **Send OTP**:

   - The user provides their email address.
   - The application generates a random OTP and stores it in memory (using a `Map`).
   - The OTP is sent to the user's email.

2. **Verify OTP**:
   - The user provides their email and the OTP they received.
   - The application checks if the OTP matches the one stored in memory.
   - If it matches, the OTP is removed from memory, and verification is successful.

## Notes

- This implementation uses an in-memory store (`Map`) for simplicity. In a real-world application, you would use a database to store OTPs.
- The OTP expires only when removed manually. You can enhance this by adding a timestamp and checking expiration.
- Handle exceptions and edge cases properly in production.

## Next Steps

- Explore how to use a database for OTP storage.
- Learn about securing endpoints using Spring Security.
- Implement rate-limiting to prevent abuse of the OTP feature.

Happy learning!
