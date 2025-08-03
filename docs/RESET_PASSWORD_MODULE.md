# Reset Password Module in Spring Boot

This document explains the logic and implementation of the reset password functionality in the Spring Boot application. It is designed to help beginners understand the flow and logic behind the feature.

---

## **Overview**

The reset password module allows users to reset their password securely. It consists of two main endpoints:

1. **Forget Password Endpoint (`/forget-password`)**:

   - Generates a unique reset token.
   - Sends a reset link to the user's email.

2. **Reset Password Endpoint (`/reset-password`)**:
   - Accepts the reset token and new password.
   - Validates the token and updates the user's password.

---

## **Forget Password Endpoint**

### **Purpose**

This endpoint is used to initiate the password reset process. It generates a reset token and sends a reset link to the user's email.

### **Logic**

1. **Input**:

   - Accepts a `Map<String, String>` request body containing the `input` key.
   - The `input` can be either a username or an email.

2. **User Lookup**:

   - If the `input` contains `@`, it is treated as an email.
   - Otherwise, it is treated as a username.
   - The system fetches the user from the database using the `UserRepository`.

3. **Token Generation**:

   - A unique reset token is generated using `UUID.randomUUID()`.
   - The token is stored in `otpStore` (a `HashMap`) with the username as the value.

4. **Reset Link**:
   - A reset link is created using the token: `http://localhost:8080/public/reset-password?token=<resetToken>`.
   - The link is sent to the user's email using the `EmailService`.

### **Code**

```java
@PostMapping("/forget-password")
public String forgetPassword(@RequestBody Map<String, String> request) {
    if (!request.containsKey("input") || request.get("input") == null) {
        return "Invalid request. 'input' is required.";
    }

    String input = request.get("input");
    User user = null;

    if (input.contains("@")) { // Check if the input is an email
        user = userRepository.findByEmail(input);
    } else { // Assume input is a username
        user = userRepository.findByUserName(input);
    }

    if (user == null) {
        return "User not found.";
    }

    String resetToken = UUID.randomUUID().toString(); // Generate unique token
    otpStore.put(resetToken, user.getUserName()); // Store token with username
    String resetLink = "http://localhost:8080/public/reset-password?token=" + resetToken;
    emailService.sendResetLinkEmail(user.getEmail(), resetLink); // Send reset link
    return "Reset link sent to " + user.getEmail();
}
```

---

## **Reset Password Endpoint**

### **Purpose**

This endpoint is used to reset the user's password using the reset token.

### **Logic**

1. **Input**:

   - Accepts the reset token as a query parameter (`token`).
   - Accepts the new password in the request body (`newPassword`).

2. **Token Validation**:

   - Checks if the token exists in `otpStore`.
   - If the token is invalid or expired, an error message is returned.

3. **User Lookup**:

   - Fetches the username associated with the token from `otpStore`.
   - Retrieves the user from the database using the `UserRepository`.

4. **Password Update**:

   - Hashes the new password using `BCryptPasswordEncoder`.
   - Updates the user's password in the database.

5. **Token Removal**:
   - Removes the token from `otpStore` after successful password reset.

### **Code**

```java
@PostMapping("/reset-password")
public String resetPassword(@RequestParam("token") String token, @RequestBody Map<String, String> request) {
    String newPassword = request.get("newPassword");

    if (!otpStore.containsKey(token)) {
        return "Invalid or expired reset token.";
    }

    String username = otpStore.get(token);
    User user = userRepository.findByUserName(username);
    if (user == null) {
        return "User not found.";
    }

    // Hash the password before saving
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user); // Save updated user
    otpStore.remove(token); // Remove token after successful reset
    return "Password updated successfully.";
}
```

---

## **Email Service**

### **Purpose**

The `EmailService` is responsible for sending the reset link to the user's email.

### **Logic**

1. **Input**:

   - Accepts the user's email and the reset link.

2. **Email Content**:
   - Constructs the email subject and message.
   - Sends the email using the configured email service.

### **Code**

```java
public void sendResetLinkEmail(String email, String resetLink) {
    String subject = "Password Reset Request";
    String message = "Click the link below to reset your password:\n" + resetLink;
    // Logic to send email
}
```

---

## **Security Enhancements**

1. **Token Expiry**:

   - Add an expiration time for the reset token.
   - Use a library like Redis for token storage with TTL (Time-To-Live).

2. **Secure Reset Link**:

   - Use HTTPS for the reset link.
   - Validate the token securely.

3. **Rate Limiting**:
   - Limit the number of reset requests per user to prevent abuse.

---

## **Testing the Flow**

1. **Forget Password**:

   - Call `/forget-password` with a valid username or email.
   - Verify the reset link is sent via email.

2. **Reset Password**:
   - Use the reset link to call `/reset-password` with the token and new password.
   - Verify the password is updated securely.

---

## **Conclusion**

This reset password module provides a secure and user-friendly way to reset passwords. By following the logic and code provided, you can implement and enhance this feature in your Spring Boot application.
