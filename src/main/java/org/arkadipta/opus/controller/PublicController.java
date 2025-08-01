package org.arkadipta.opus.controller;

import org.arkadipta.opus.dto.QuoteResponse;
import org.arkadipta.opus.entity.User;
import org.arkadipta.opus.service.QuoteService;
import org.arkadipta.opus.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/public")
public class PublicController {

    @Autowired
    private UserService userService;


     @Autowired
    private QuoteService quoteService;

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        QuoteResponse quote = quoteService.getRandomQuote();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "i am helthy");
        response.put("quote", quote);
        return response;
    }

    @PostMapping("/create-user")
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }
}
