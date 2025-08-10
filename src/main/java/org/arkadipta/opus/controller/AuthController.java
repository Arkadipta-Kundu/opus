package org.arkadipta.opus.controller;

import jakarta.validation.Valid;
import org.arkadipta.opus.dto.JwtResponse;
import org.arkadipta.opus.dto.LoginRequest;
import org.arkadipta.opus.dto.RefreshTokenRequest;
import org.arkadipta.opus.entity.User;
import org.arkadipta.opus.repository.UserRepository;
import org.arkadipta.opus.service.EmailService;
import org.arkadipta.opus.service.UserDetailsServiceImpl;
import org.arkadipta.opus.service.UserService;
import org.arkadipta.opus.util.JwtUtil;
import org.arkadipta.opus.util.OtpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

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
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // Get user details
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByUserName(userDetails.getUsername());

            // Generate tokens
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("email", user.getEmail());
            claims.put("roles", user.getRoles());

            String jwt = jwtUtil.generateToken(userDetails.getUsername(), claims);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername());

            // Prepare response
            JwtResponse jwtResponse = new JwtResponse();
            jwtResponse.setToken(jwt);
            jwtResponse.setRefreshToken(refreshToken);
            jwtResponse.setUsername(user.getUserName());
            jwtResponse.setEmail(user.getEmail());
            jwtResponse.setRoles(user.getRoles());
            jwtResponse.setExpiresIn(86400L); // 24 hours in seconds

            return ResponseEntity.ok(jwtResponse);

        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid username or password"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }
    @PostMapping("/user-varification/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshRequest) {
        try {
            String refreshToken = refreshRequest.getRefreshToken();

            // Validate refresh token
            if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid refresh token"));
            }

            // Extract username and generate new access token
            String username = jwtUtil.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            User user = userRepository.findByUserName(username);

            // Generate a new access token
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("email", user.getEmail());
            claims.put("roles", user.getRoles());

            String newAccessToken = jwtUtil.generateToken(username, claims);

            return ResponseEntity.ok(Map.of(
                    "token", newAccessToken,
                    "type", "Bearer",
                    "expiresIn", 86400L
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Token refresh failed: " + e.getMessage()));
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
