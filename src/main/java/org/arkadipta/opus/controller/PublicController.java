package org.arkadipta.opus.controller;

import org.arkadipta.opus.dto.QuoteResponse;
import org.arkadipta.opus.entity.User;
import org.arkadipta.opus.repository.UserRepository;
import org.arkadipta.opus.service.EmailService;
import org.arkadipta.opus.service.QuoteService;
import org.arkadipta.opus.service.UserService;
import org.arkadipta.opus.util.OtpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/public")
public class PublicController {

    @Autowired
    private UserService userService;
    @Autowired
    private OtpUtil otpUtil;

    @Autowired
    private QuoteService quoteService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    // Store reset tokens mapped to usernames for password reset
    private Map<String, String> otpStore = new HashMap<>();

    /**
     * Health check endpoint.
     * Returns a status and a random quote.
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        QuoteResponse quote = quoteService.getRandomQuote();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "i am helthy");
        response.put("quote", quote);
        return response;
    }

    /**
     * Create a new user.
     * @param user User object from request body
     * @return Created User
     */
    @PostMapping("/create-user")
    public User createUser(@RequestBody User user) {
        try {
            return userService.createUser(user);
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Initiate password reset process.
     * Accepts either email or username as input.
     * Sends a reset link to the user's email.
     */
    @PostMapping("/forget-password")
    public String forgetPassword(@RequestBody Map<String, String> request) {
        if (!request.containsKey("input") || request.get("input") == null) {
            return "Invalid request. 'input' is required.";
        }

        String input = request.get("input");
        User user = null;

        // Determine if input is email or username
        if (input.contains("@")) { // Check if the input is an email
            user = userRepository.findByEmail(input);
        } else { // Assume input is a username
            user = userRepository.findByUserName(input);
        }

        if (user == null) {
            return "User not found.";
        }

        // Generate a unique reset token and store it
        String resetToken = UUID.randomUUID().toString(); // Generate unique token
        otpStore.put(resetToken, user.getUserName()); // Store token with username

        // Build reset link and send email
        String resetLink = "http://localhost:8080/public/reset-password?token=" + resetToken;
        emailService.sendResetLinkEmail(user.getEmail(), resetLink); // Send reset link

        return "Reset link sent to " + user.getEmail();
    }

    /**
     * Reset password using the token sent to user's email.
     * @param token Reset token from query parameter
     * @param request Request body containing new password
     * @return Status message
     */
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("token") String token, @RequestBody Map<String, String> request) {
        String newPassword = request.get("newPassword");

        // Validate token
        if (!otpStore.containsKey(token)) {
            return "Invalid or expired reset token.";
        }

        // Retrieve user by username stored in otpStore
        String username = otpStore.get(token);
        User user = userRepository.findByUserName(username);
        if (user == null) {
            return "User not found.";
        }

        // Hash the password before saving
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // Update user's password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user); // Save updated user

        // Remove token after successful reset
        otpStore.remove(token);

        return "Password updated successfully.";
    }
}
