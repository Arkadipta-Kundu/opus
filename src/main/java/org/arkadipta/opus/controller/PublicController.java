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
}
