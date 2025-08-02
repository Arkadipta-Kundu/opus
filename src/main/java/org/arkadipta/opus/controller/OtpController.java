package org.arkadipta.opus.controller;

import org.arkadipta.opus.entity.User;
import org.arkadipta.opus.repository.UserRepository;
import org.arkadipta.opus.service.EmailService;
import org.arkadipta.opus.service.UserService;
import org.arkadipta.opus.util.OtpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user-varification")
public class OtpController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private OtpUtil otpUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private Map<String, String> otpStore = new HashMap<>();

    @PostMapping("/send")
    public String sendOtp(Principal principal) {
        String username = principal.getName(); // Get the logged-in user's username
        User user = userRepository.findByUserName(username);
        if (user == null) {
            return "User not found.";
        }
        String email = user.getEmail();
        String otp = otpUtil.generateOtp();
        otpStore.put(email, otp);
        emailService.sendOtpEmail(email, otp);
        return "OTP sent to " + email;
    }

    @PostMapping("/verify")
    public String verifyOtp(@RequestParam String otp, Principal principal) {
        String username = principal.getName(); // Get the logged-in user's username

        User user = userRepository.findByUserName(username);
        if (user == null) {
            return "User not found.";
        }
        String email = user.getEmail();

        String storedOtp = otpStore.get(email);

        if (storedOtp != null && storedOtp.equals(otp)) {
            otpStore.remove(email);

            // Mark email as verified
            user.setEmailVerified(true);
            userRepository.save(user);

            return "Email verified successfully!";
        } else {
            return "Invalid OTP or OTP expired.";
        }
    }

    @GetMapping("/is-verified")
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
}
