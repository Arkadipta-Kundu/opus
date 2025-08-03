# Using Redis for OTP and Token Storage in Spring Boot

## Overview

This guide explains how to replace the `HashMap` used for OTP and token storage in your `AuthController` with Redis. Redis provides a robust, scalable, and efficient way to store temporary data such as OTPs and tokens.

## Why Use Redis?

- **Scalability**: Redis can handle large-scale applications with ease.
- **TTL Support**: Redis allows setting expiration times for keys, making it ideal for temporary data.
- **Persistence**: Redis can optionally persist data to disk.
- **Atomic Operations**: Redis ensures data consistency.

## Steps to Replace HashMap with Redis

### 1. Add Redis Dependencies

Update your `pom.xml` to include Redis dependencies:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
</dependencies>
```

### 2. Configure Redis Connection

Add Redis configuration to your `application.properties`:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.database=0
spring.data.redis.timeout=60000ms
```

### 3. Create RedisTemplate Bean

Create a configuration class to define the `RedisTemplate` bean:

```java
package org.arkadipta.opus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

### 4. Update AuthController to Use Redis

Replace the `HashMap` with Redis for OTP and token storage:

#### Inject RedisTemplate

```java
@Autowired
private RedisTemplate<String, String> redisTemplate;
```

#### Replace OTP Storage Logic

**Send OTP:**

```java
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
```

**Verify OTP:**

```java
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
```

#### Replace Token Storage Logic

**Forget Password:**

```java
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
```

**Reset Password:**

```java
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
```

### 5. Test Your Implementation

#### Unit Tests

```java
@SpringBootTest
class AuthControllerTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void testOtpStorage() {
        redisTemplate.opsForValue().set("otp:test@example.com", "123456", 5, TimeUnit.MINUTES);
        String otp = redisTemplate.opsForValue().get("otp:test@example.com");
        assertEquals("123456", otp);
    }

    @Test
    void testTokenStorage() {
        redisTemplate.opsForValue().set("reset-token:abc123", "testUser", 1, TimeUnit.HOURS);
        String username = redisTemplate.opsForValue().get("reset-token:abc123");
        assertEquals("testUser", username);
    }
}
```

#### Integration Tests

```java
@SpringBootTest
@Testcontainers
class RedisIntegrationTest {

    @Container
    static RedisContainer redis = new RedisContainer("redis:alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void testRedisConnection() {
        redisTemplate.opsForValue().set("test", "value");
        String result = redisTemplate.opsForValue().get("test");
        assertEquals("value", result);
    }
}
```

## Summary

By replacing the `HashMap` with Redis, you gain scalability, TTL support, and better performance for OTP and token storage. Follow the steps above to integrate Redis into your `AuthController` and test the implementation thoroughly.
