package org.arkadipta.opus.controller;

import org.arkadipta.opus.entity.User;
import org.arkadipta.opus.repository.UserRepository;
import org.arkadipta.opus.service.EmailService;
import org.arkadipta.opus.service.UserService;
import org.arkadipta.opus.util.OtpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private OtpUtil otpUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Create a new user.
     *
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

    @PostMapping("/user-varification/login")
    public ResponseEntity<Boolean> login(@RequestParam String userName, String password) {
        User user = userRepository.findByUserName(userName);
        if (user == null) {
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
        String userPassFromDb = user.getPassword();

        if (passwordEncoder.matches(password, userPassFromDb)) {
            return new ResponseEntity<>(true, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/user-varification/send")
    public String sendOtp(Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByUserName(username);
        if (user == null) {
            return "User not found.";
        }
        String email = user.getEmail();
        String otp = otpUtil.generateOtp();

        // Store OTP in Redis with a TTL of 5 minutes
        redisTemplate.opsForValue().set("otp:" + email, otp, 5, TimeUnit.MINUTES);

        emailService.sendOtpEmail(email, otp);
        return "OTP sent to " + email;
    }

    @PostMapping("/user-varification/verify")
    public String verifyOtp(@RequestParam String otp, Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByUserName(username);
        if (user == null) {
            return "User not found.";
        }
        String email = user.getEmail();

        // Retrieve OTP from Redis
        String storedOtp = redisTemplate.opsForValue().get("otp:" + email);

        if (storedOtp != null && storedOtp.equals(otp)) {
            // Remove OTP from Redis
            redisTemplate.delete("otp:" + email);

            // Mark email as verified
            user.setEmailVerified(true);
            userRepository.save(user);

            return "Email verified successfully!";
        } else {
            return "Invalid OTP or OTP expired.";
        }
    }

    @GetMapping("/user-varification/is-verified")
    public ResponseEntity<Boolean> isEmailVerified(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
        }
        String username = principal.getName(); // Get the logged-in user's username
        User user = userRepository.findByUserName(username); // Retrieve the user by username
        if (user != null) {
            boolean isVerified = userService.isEmailVerified(user.getEmail()); // Check email verification
            return ResponseEntity.ok(isVerified);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
        }
    }

    /**
     * Initiate password reset process.
     * Accepts either email or username as input.
     * Sends a reset link to the user's email.
     */
    @PostMapping("/user-varification/forget-password")
    public String forgetPassword(@RequestBody Map<String, String> request) {
        if (!request.containsKey("input") || request.get("input") == null) {
            return "Invalid request. 'input' is required.";
        }

        String input = request.get("input");
        User user = null;

        if (input.contains("@")) {
            user = userRepository.findByEmail(input);
        } else {
            user = userRepository.findByUserName(input);
        }

        if (user == null) {
            return "User not found.";
        }

        String resetToken = UUID.randomUUID().toString();

        // Store token in Redis with a TTL of 1 hour
        redisTemplate.opsForValue().set("reset-token:" + resetToken, user.getUserName(), 1, TimeUnit.HOURS);

        String resetLink = "http://localhost:8080/auth/user-varification/reset-password?token=" + resetToken;
        emailService.sendResetLinkEmail(user.getEmail(), resetLink);

        return "Reset link sent to " + user.getEmail();
    }

    /**
     * Reset password using the token sent to user's email.
     *
     * @param token   Reset token from query parameter
     * @param request Request body containing new password
     * @return Status message
     */
    @PostMapping("/user-varification/reset-password")
    public String resetPassword(@RequestParam("token") String token, @RequestBody Map<String, String> request) {
        String newPassword = request.get("newPassword");

        // Validate token
        String username = redisTemplate.opsForValue().get("reset-token:" + token);
        if (username == null) {
            return "Invalid or expired reset token.";
        }

        User user = userRepository.findByUserName(username);
        if (user == null) {
            return "User not found.";
        }

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Remove token from Redis
        redisTemplate.delete("reset-token:" + token);

        return "Password updated successfully.";
    }

}
