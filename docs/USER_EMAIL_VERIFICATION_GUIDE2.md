# User Email Verification Guide

This guide explains how to incorporate email verification into your current system using the existing `EmailService` and `OtpController`. The goal is to verify a user's email address during registration or other actions requiring validation.

## Overview

Email verification ensures that the user owns the email address they provide. The process involves:

1. Generating an OTP.
2. Sending the OTP to the user's email.
3. Storing the OTP temporarily.
4. Verifying the OTP entered by the user.

## Step 1: Update the `User` Entity

Add a field to the `User` entity to track whether the email is verified:

```java
@Entity
@Table(name = "users")
public class User {
    // ...existing code...

    private boolean emailVerified = false;

    // Getter and Setter for emailVerified
    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}
```

## Step 2: Update the `OtpController`

Modify the `OtpController` to include email verification logic:

### Send OTP

Ensure the `/send` endpoint generates and sends an OTP:

```java
@PostMapping("/send")
public String sendOtp(@RequestParam String email) {
    String otp = otpUtil.generateOtp();
    otpStore.put(email, otp);
    emailService.sendOtpEmail(email, otp);
    return "OTP sent to " + email;
}
```

### Verify OTP

Update the `/verify` endpoint to mark the user's email as verified:

```java
@PostMapping("/verify")
public String verifyOtp(@RequestParam String email, @RequestParam String otp) {
    String storedOtp = otpStore.get(email);
    if (storedOtp != null && storedOtp.equals(otp)) {
        otpStore.remove(email);

        // Mark email as verified
        User user = userRepository.findByEmail(email);
        if (user != null) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        return "Email verified successfully!";
    } else {
        return "Invalid OTP or OTP expired.";
    }
}
```

## Step 3: Update the `UserService`

Add a method to check if a user's email is verified:

```java
public boolean isEmailVerified(String email) {
    User user = userRepository.findByEmail(email);
    return user != null && user.isEmailVerified();
}
```

## Step 4: Test the Application

1. Start your Spring Boot application.
2. Use Postman or another tool to test the endpoints:
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
   - If it matches, the OTP is removed from memory, and the user's email is marked as verified.

## Notes

- This implementation uses an in-memory store (`Map`) for simplicity. In a real-world application, you would use a database to store OTPs.
- Handle exceptions and edge cases properly in production.
- Add expiration logic for OTPs to enhance security.

## Next Steps

- Implement rate-limiting to prevent abuse of the OTP feature.
- Use a database to store OTPs and track expiration.
- Secure endpoints using Spring Security.

Happy learning!
