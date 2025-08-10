# JWT Implementation Guide for Spring Boot Opus Application

## Table of Contents

1. [JWT Theory and Concepts](#jwt-theory-and-concepts)
2. [Why JWT in Your Application](#why-jwt-in-your-application)
3. [Current Authentication Analysis](#current-authentication-analysis)
4. [JWT Implementation Strategy](#jwt-implementation-strategy)
5. [Dependencies Required](#dependencies-required)
6. [Code Implementation](#code-implementation)
7. [Security Configuration](#security-configuration)
8. [Testing the Implementation](#testing-the-implementation)
9. [Best Practices and Security](#best-practices-and-security)
10. [Migration from Current Auth System](#migration-from-current-auth-system)

---

## JWT Theory and Concepts

### What is JWT?

**JWT (JSON Web Token)** is a compact, URL-safe means of representing claims to be transferred between two parties. It's a self-contained token that carries information about the user and can be verified without storing session state on the server.

### JWT Structure

A JWT consists of three parts separated by dots (`.`):

```
header.payload.signature
```

#### 1. Header

Contains metadata about the token:

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

#### 2. Payload

Contains claims (statements about an entity):

```json
{
  "sub": "user123",
  "name": "John Doe",
  "role": "USER",
  "iat": 1516239022,
  "exp": 1516242622
}
```

#### 3. Signature

Ensures token integrity:

```
HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret
)
```

### JWT Types of Claims

1. **Registered Claims**: Predefined claims (iss, exp, sub, aud, etc.)
2. **Public Claims**: Can be defined at will but should be collision-resistant
3. **Private Claims**: Custom claims for sharing information between parties

### JWT Workflow

```
1. User Login → 2. Server Validates → 3. Generate JWT → 4. Return JWT
     ↓                                                         ↑
5. Store JWT → 6. Include in Requests → 7. Server Validates → 8. Process Request
```

---

## Why JWT in Your Application

### Current Limitations

Your current authentication system has several limitations:

- **Stateful Authentication**: Uses server-side sessions
- **Scalability Issues**: Hard to scale across multiple servers
- **Limited API Support**: Basic authentication isn't ideal for modern SPAs/mobile apps
- **No Token Expiration**: Manual session management required

### JWT Benefits for Opus Application

1. **Stateless**: No server-side session storage needed
2. **Scalable**: Works across multiple servers/microservices
3. **Self-contained**: Token carries user information
4. **Secure**: Cryptographically signed
5. **Standard**: Industry standard for API authentication
6. **Mobile-friendly**: Perfect for mobile applications
7. **Cross-domain**: Can be used across different domains

---

## Current Authentication Analysis

### Existing System Overview

Based on your codebase analysis:

```java
// Current Basic Authentication
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
```

### Issues with Current System

1. **Returns only Boolean**: No user context or token
2. **No Session Management**: No way to maintain authentication state
3. **No Role-based Access**: Limited authorization capabilities
4. **No Token Expiration**: No automatic logout mechanism

---

## JWT Implementation Strategy

### Implementation Phases

#### Phase 1: Core JWT Infrastructure

- JWT utility classes
- Token generation and validation
- JWT authentication filter

#### Phase 2: Security Configuration

- Update Spring Security configuration
- JWT-based authentication
- Role-based authorization

#### Phase 3: Controller Updates

- Update login endpoints
- Add token refresh mechanism
- Update existing endpoints

#### Phase 4: Advanced Features

- Token blacklisting
- Remember me functionality
- Multi-device login support

---

## Dependencies Required

### Maven Dependencies

Add these to your `pom.xml`:

```xml
<!-- JWT Dependencies -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- Additional Security (Optional but recommended) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### Why These Dependencies?

- **jjwt-api**: Core JWT API
- **jjwt-impl**: JWT implementation
- **jjwt-jackson**: JSON processing for JWT
- **validation**: For request validation

---

## Code Implementation

### 1. JWT Utility Class

```java
package org.arkadipta.opus.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret:mySecretKey}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private Long expiration;

    @Value("${jwt.refresh-expiration:604800000}") // 7 days in milliseconds
    private Long refreshExpiration;

    // Generate secret key
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Extract username from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract expiration date from token
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Extract specific claim from token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Extract all claims from token
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Check if token is expired
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Generate token for user
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername(), expiration);
    }

    // Generate token with custom claims
    public String generateToken(String username, Map<String, Object> claims) {
        return createToken(claims, username, expiration);
    }

    // Generate refresh token
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return createToken(claims, username, refreshExpiration);
    }

    // Create token with claims and expiration
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    // Validate token
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Validate token without UserDetails
    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Extract roles from token
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    // Check if token is refresh token
    public Boolean isRefreshToken(String token) {
        Claims claims = extractAllClaims(token);
        return "refresh".equals(claims.get("type"));
    }
}
```

### 2. JWT Authentication Filter

```java
package org.arkadipta.opus.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.arkadipta.opus.service.CustomUserDetailsService;
import org.arkadipta.opus.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // Extract JWT from Authorization header
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                logger.error("Cannot extract username from JWT token: " + e.getMessage());
            }
        }

        // If username is extracted and no authentication is set
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // Validate token
            if (jwtUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

### 3. Custom UserDetailsService

```java
package org.arkadipta.opus.service;

import org.arkadipta.opus.entity.User;
import org.arkadipta.opus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUserName(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUserName(),
                user.getPassword(),
                true, // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                getAuthorities(user.getRoles())
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }

    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUserName(),
                user.getPassword(),
                true,
                true,
                true,
                true,
                getAuthorities(user.getRoles())
        );
    }
}
```

### 4. Authentication Request/Response DTOs

```java
package org.arkadipta.opus.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private boolean rememberMe = false;
}
```

```java
package org.arkadipta.opus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponse {
    private String token;
    private String refreshToken;
    private String type = "Bearer";
    private String username;
    private String email;
    private List<String> roles;
    private Long expiresIn; // in seconds
}
```

```java
package org.arkadipta.opus.dto;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;
}
```

### 5. Updated Authentication Controller

```java
package org.arkadipta.opus.controller;

import jakarta.validation.Valid;
import org.arkadipta.opus.dto.*;
import org.arkadipta.opus.entity.User;
import org.arkadipta.opus.repository.UserRepository;
import org.arkadipta.opus.service.AuthService;
import org.arkadipta.opus.service.CustomUserDetailsService;
import org.arkadipta.opus.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class JwtAuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
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

    @PostMapping("/refresh")
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

            // Generate new access token
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

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        // In a production environment, you would:
        // 1. Add token to blacklist
        // 2. Invalidate refresh token
        // 3. Clear any server-side sessions

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
```

---

## Security Configuration

### Updated Spring Security Configuration

```java
package org.arkadipta.opus.config;

import org.arkadipta.opus.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class JwtSecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/auth/refresh").permitAll()
                .requestMatchers("/auth/create-user").permitAll()
                .requestMatchers("/auth/user-varification/forget-password").permitAll()
                .requestMatchers("/auth/user-varification/reset-password").permitAll()

                // Admin endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // User endpoints (require authentication)
                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/auth/user-varification/**").authenticated()

                // All other requests require authentication
                .anyRequest().authenticated())
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5500",
            "https://*.netlify.app",
            "https://*.vercel.app"
        ));

        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));

        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
```

### JWT Authentication Entry Point

```java
package org.arkadipta.opus.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", "Full authentication is required to access this resource");
        body.put("path", request.getServletPath());

        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }
}
```

---

## Testing the Implementation

### 1. Login Request

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "your_username",
    "password": "your_password"
  }'
```

**Expected Response:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "your_username",
  "email": "user@example.com",
  "roles": ["USER"],
  "expiresIn": 86400
}
```

### 2. Accessing Protected Endpoints

```bash
curl -X GET http://localhost:8080/user/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 3. Refreshing Token

```bash
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

### 4. Frontend Integration

```javascript
// Login function
async function login(username, password) {
  const response = await fetch("/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ username, password }),
  });

  if (response.ok) {
    const data = await response.json();
    localStorage.setItem("token", data.token);
    localStorage.setItem("refreshToken", data.refreshToken);
    return data;
  }

  throw new Error("Login failed");
}

// API call with token
async function apiCall(url, options = {}) {
  const token = localStorage.getItem("token");

  return fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      Authorization: `Bearer ${token}`,
    },
  });
}

// Auto-refresh token
async function refreshToken() {
  const refreshToken = localStorage.getItem("refreshToken");

  const response = await fetch("/auth/refresh", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ refreshToken }),
  });

  if (response.ok) {
    const data = await response.json();
    localStorage.setItem("token", data.token);
    return data.token;
  }

  // Refresh failed, redirect to login
  localStorage.removeItem("token");
  localStorage.removeItem("refreshToken");
  window.location.href = "/login";
}
```

---

## Best Practices and Security

### 1. Token Security

#### Secret Key Management

```properties
# application.properties
jwt.secret=${JWT_SECRET:your-256-bit-secret-key-here-must-be-long-enough}
jwt.expiration=${JWT_EXPIRATION:86400000}
jwt.refresh-expiration=${JWT_REFRESH_EXPIRATION:604800000}
```

#### Environment Variables

```bash
# .env file (for production)
JWT_SECRET=your-very-long-and-secure-secret-key-at-least-256-bits
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000
```

### 2. Token Storage Best Practices

#### Client-Side Storage Options

1. **localStorage**: Persistent but vulnerable to XSS
2. **sessionStorage**: Session-based, still vulnerable to XSS
3. **httpOnly Cookies**: More secure, not accessible via JavaScript
4. **Memory Only**: Most secure but lost on page refresh

#### Recommended Approach

```java
// For high-security applications, use httpOnly cookies
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request,
                              HttpServletResponse response) {
    // ... authentication logic

    // Set JWT in httpOnly cookie
    Cookie jwtCookie = new Cookie("jwt", jwt);
    jwtCookie.setHttpOnly(true);
    jwtCookie.setSecure(true); // HTTPS only
    jwtCookie.setPath("/");
    jwtCookie.setMaxAge(86400); // 24 hours
    response.addCookie(jwtCookie);

    return ResponseEntity.ok(jwtResponse);
}
```

### 3. Token Blacklisting

```java
@Service
public class TokenBlacklistService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void blacklistToken(String token) {
        // Extract expiration time from token
        Date expiration = jwtUtil.extractExpiration(token);
        long timeToLive = expiration.getTime() - System.currentTimeMillis();

        if (timeToLive > 0) {
            redisTemplate.opsForValue().set(
                "blacklist:" + token,
                "true",
                timeToLive,
                TimeUnit.MILLISECONDS
            );
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return redisTemplate.hasKey("blacklist:" + token);
    }
}
```

### 4. Rate Limiting

```java
@Component
public class LoginAttemptService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION = 15 * 60; // 15 minutes

    public boolean isBlocked(String username) {
        String attempts = redisTemplate.opsForValue().get("login_attempts:" + username);
        return attempts != null && Integer.parseInt(attempts) >= MAX_ATTEMPTS;
    }

    public void recordFailedAttempt(String username) {
        String key = "login_attempts:" + username;
        String attempts = redisTemplate.opsForValue().get(key);

        if (attempts == null) {
            redisTemplate.opsForValue().set(key, "1", LOCKOUT_DURATION, TimeUnit.SECONDS);
        } else {
            int currentAttempts = Integer.parseInt(attempts);
            redisTemplate.opsForValue().set(
                key,
                String.valueOf(currentAttempts + 1),
                LOCKOUT_DURATION,
                TimeUnit.SECONDS
            );
        }
    }

    public void resetFailedAttempts(String username) {
        redisTemplate.delete("login_attempts:" + username);
    }
}
```

### 5. Security Headers

```java
@Configuration
public class SecurityHeaderConfig {

    @Bean
    public FilterRegistrationBean<SecurityHeaderFilter> securityHeaderFilter() {
        FilterRegistrationBean<SecurityHeaderFilter> registrationBean =
            new FilterRegistrationBean<>();

        registrationBean.setFilter(new SecurityHeaderFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);

        return registrationBean;
    }
}

@Component
public class SecurityHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                        FilterChain chain) throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Security headers
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Strict-Transport-Security",
            "max-age=31536000; includeSubDomains");
        httpResponse.setHeader("Content-Security-Policy",
            "default-src 'self'");

        chain.doFilter(request, response);
    }
}
```

---

## Migration from Current Auth System

### Migration Strategy

#### Phase 1: Parallel Implementation

1. Keep existing authentication working
2. Add JWT endpoints alongside current ones
3. Test JWT implementation thoroughly

#### Phase 2: Frontend Migration

1. Update frontend to use JWT endpoints
2. Implement token storage and refresh logic
3. Handle authentication state properly

#### Phase 3: Deprecation

1. Mark old endpoints as deprecated
2. Monitor usage and migrate remaining clients
3. Remove old authentication system

### Migration Checklist

#### Backend Changes

- [ ] Add JWT dependencies to `pom.xml`
- [ ] Create JWT utility classes
- [ ] Implement JWT authentication filter
- [ ] Create custom UserDetailsService
- [ ] Update security configuration
- [ ] Create new authentication endpoints
- [ ] Add proper error handling
- [ ] Implement token refresh mechanism
- [ ] Add rate limiting and security measures

#### Frontend Changes

- [ ] Update login API calls
- [ ] Implement token storage strategy
- [ ] Add authentication interceptors
- [ ] Handle token expiration and refresh
- [ ] Update routing/navigation logic
- [ ] Add logout functionality
- [ ] Handle authentication errors gracefully

#### Testing

- [ ] Unit tests for JWT utilities
- [ ] Integration tests for authentication endpoints
- [ ] Security tests for protected endpoints
- [ ] Performance tests for token validation
- [ ] End-to-end tests for complete flows

#### Production Considerations

- [ ] Secure secret key management
- [ ] HTTPS enforcement
- [ ] Token blacklisting setup
- [ ] Monitoring and logging
- [ ] Backup authentication method
- [ ] Documentation updates

### Example Migration Script

```java
@Component
public class AuthMigrationService {

    public void migrateUserSessions() {
        // If you have existing sessions, you might want to:
        // 1. Generate JWT tokens for active sessions
        // 2. Store mapping between old session IDs and new JWTs
        // 3. Gradually phase out old sessions
    }

    public void validateMigration() {
        // Validation logic to ensure migration is successful
        // Compare authentication success rates before/after
        // Monitor error rates and user experience
    }
}
```

---

## Conclusion

This comprehensive guide provides everything you need to implement JWT authentication in your Spring Boot Opus application. The implementation includes:

1. **Secure JWT generation and validation**
2. **Stateless authentication**
3. **Role-based authorization**
4. **Token refresh mechanism**
5. **Security best practices**
6. **Migration strategy from your current system**

### Next Steps

1. Review and understand the code examples
2. Implement the changes incrementally
3. Test thoroughly in a development environment
4. Plan your migration strategy
5. Deploy to production with proper monitoring

### Additional Resources

- [JWT.io](https://jwt.io/) - JWT debugger and documentation
- [OWASP JWT Security Guide](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [Spring Security JWT Reference](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

Remember to always prioritize security and follow best practices when implementing authentication systems in production applications.
