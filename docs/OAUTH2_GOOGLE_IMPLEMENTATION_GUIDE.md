# OAuth2 Google Integration Guide for Spring Boot

## Table of Contents

1. [OAuth2 Theory and Concepts](#oauth2-theory-and-concepts)
2. [Why OAuth2 with Google](#why-oauth2-with-google)
3. [Prerequisites](#prerequisites)
4. [Google Cloud Console Setup](#google-cloud-console-setup)
5. [Spring Boot Dependencies](#spring-boot-dependencies)
6. [Configuration Setup](#configuration-setup)
7. [Implementation](#implementation)
8. [Security Configuration](#security-configuration)
9. [Frontend Integration](#frontend-integration)
10. [Testing the Implementation](#testing-the-implementation)
11. [Advanced Features](#advanced-features)
12. [Production Considerations](#production-considerations)
13. [Troubleshooting](#troubleshooting)

---

## OAuth2 Theory and Concepts

### What is OAuth2?

**OAuth2** (Open Authorization 2.0) is an authorization framework that enables applications to obtain limited access to user accounts on an HTTP service. It allows users to grant third-party applications access to their resources without sharing their credentials.

### OAuth2 Roles

1. **Resource Owner**: The user who owns the data
2. **Client**: Your Spring Boot application
3. **Authorization Server**: Google's OAuth2 server
4. **Resource Server**: Google's API servers (Gmail, Drive, etc.)

### OAuth2 Flow (Authorization Code Grant)

```
1. User clicks "Login with Google"
2. Redirect to Google's authorization server
3. User logs in and grants permissions
4. Google redirects back with authorization code
5. Your app exchanges code for access token
6. Use access token to access user's Google data
```

### OAuth2 vs Traditional Authentication

| Traditional Auth              | OAuth2                     |
| ----------------------------- | -------------------------- |
| User enters username/password | User logs in via Google    |
| Store password hash           | No password storage needed |
| Manage user credentials       | Google manages credentials |
| Limited user info             | Rich user profile data     |
| Single sign-on complexity     | Built-in SSO support       |

---

## Why OAuth2 with Google

### Benefits for Your Opus Application

1. **Enhanced User Experience**

   - No registration required
   - One-click login
   - Familiar Google interface

2. **Security Advantages**

   - No password storage
   - Google's security infrastructure
   - Two-factor authentication support

3. **Rich User Data**

   - Email (verified by Google)
   - Profile picture
   - Name and basic info

4. **Reduced Development Effort**

   - No password reset flows
   - No email verification needed
   - Automatic user profile updates

5. **Trust and Convenience**
   - Users trust Google
   - Single sign-on across applications
   - Mobile app integration ready

---

## Prerequisites

### What You Need

1. **Google Account** - For Google Cloud Console access
2. **Spring Boot Application** - Your existing Opus project
3. **Domain/Localhost** - For redirect URLs
4. **Basic Understanding** - Spring Security, REST APIs

### Knowledge Requirements

- Basic Spring Boot concepts
- Understanding of HTTP redirects
- Basic knowledge of JSON and REST APIs
- Familiarity with Spring Security (helpful but not required)

---

## Google Cloud Console Setup

### Step 1: Create a Google Cloud Project

1. **Go to Google Cloud Console**

   - Visit: https://console.cloud.google.com/
   - Sign in with your Google account

2. **Create New Project**

   ```
   1. Click "Select a project" dropdown
   2. Click "New Project"
   3. Project Name: "Opus OAuth2"
   4. Click "Create"
   ```

3. **Select Your Project**
   - Ensure your new project is selected in the dropdown

### Step 2: Enable Google+ API

1. **Navigate to APIs & Services**

   ```
   1. Left sidebar → "APIs & Services" → "Library"
   2. Search for "Google+ API"
   3. Click on "Google+ API"
   4. Click "Enable"
   ```

2. **Alternative: Enable People API** (Recommended)
   ```
   1. Search for "People API"
   2. Click "Enable"
   ```

### Step 3: Create OAuth2 Credentials

1. **Go to Credentials**

   ```
   1. Left sidebar → "APIs & Services" → "Credentials"
   2. Click "Create Credentials"
   3. Select "OAuth client ID"
   ```

2. **Configure OAuth Consent Screen** (First time only)

   ```
   1. Click "Configure Consent Screen"
   2. Select "External" for user type
   3. Fill in required fields:
      - App name: "Opus Application"
      - User support email: Your email
      - Developer contact: Your email
   4. Click "Save and Continue"
   5. Skip Scopes and Test Users (for now)
   6. Click "Back to Dashboard"
   ```

3. **Create OAuth Client ID**

   ```
   1. Application type: "Web application"
   2. Name: "Opus Web Client"
   3. Authorized redirect URIs:
      - http://localhost:8080/login/oauth2/code/google
      - http://localhost:8080/oauth2/callback/google
   4. Click "Create"
   ```

4. **Save Credentials**
   ```
   - Copy Client ID: e.g., "123456789-abcdef.apps.googleusercontent.com"
   - Copy Client Secret: e.g., "GOCSPX-abcdef123456"
   - Keep these secure!
   ```

### Step 4: OAuth Consent Screen Details

**Required Information:**

```
App Information:
- App name: Opus Task Manager
- App logo: (Optional) Upload your app logo
- App domain: http://localhost:8080 (for development)

Developer Contact:
- Email: your-email@gmail.com

Authorized Domains:
- localhost (for development)
- your-production-domain.com (for production)
```

---

## Spring Boot Dependencies

### Add OAuth2 Dependencies to pom.xml

```xml
<!-- OAuth2 Client Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<!-- Spring Security (if not already present) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Web Starter (if not already present) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- For JSON processing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-json</artifactId>
</dependency>

<!-- Optional: For WebClient (modern HTTP client) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

### Complete Dependency Section

```xml
<dependencies>
    <!-- Your existing dependencies -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- OAuth2 Dependencies -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-client</artifactId>
    </dependency>

    <!-- JWT Dependencies (your existing) -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.6</version>
    </dependency>

    <!-- Other existing dependencies... -->
</dependencies>
```

---

## Configuration Setup

### application.properties Configuration

Add these OAuth2 settings to your `application.properties`:

```properties
# Existing configurations...
spring.application.name=opus

# OAuth2 Google Configuration
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
spring.security.oauth2.client.registration.google.scope=openid,profile,email
spring.security.oauth2.client.registration.google.redirect-uri=http://localhost:8080/login/oauth2/code/google
spring.security.oauth2.client.registration.google.client-name=Google

# OAuth2 Provider Configuration
spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/auth
spring.security.oauth2.client.provider.google.token-uri=https://oauth2.googleapis.com/token
spring.security.oauth2.client.provider.google.user-info-uri=https://openidconnect.googleapis.com/v1/userinfo
spring.security.oauth2.client.provider.google.user-name-attribute=sub

# Application URLs
app.base-url=http://localhost:8080
app.frontend-url=http://localhost:3000

# OAuth2 Success/Failure URLs
oauth2.success-redirect=/oauth2/success
oauth2.failure-redirect=/oauth2/failure
```

### Environment Variables Setup

For security, use environment variables in production:

```bash
# .env file or environment variables
GOOGLE_CLIENT_ID=123456789-abcdef.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-abcdef123456
```

Update `application.properties` to use environment variables:

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID:your-default-client-id}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET:your-default-secret}
```

---

## Implementation

### 1. OAuth2 User Model

Create a model to represent OAuth2 user information:

```java
package org.arkadipta.opus.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2UserInfo {
    private String id;
    private String name;
    private String email;
    private String imageUrl;
    private Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.id = (String) attributes.get("sub");
        this.name = (String) attributes.get("name");
        this.email = (String) attributes.get("email");
        this.imageUrl = (String) attributes.get("picture");
    }
}
```

### 2. Custom OAuth2 User Service

Create a service to handle OAuth2 user processing:

```java
package org.arkadipta.opus.service;

import org.arkadipta.opus.dto.OAuth2UserInfo;
import org.arkadipta.opus.entity.User;
import org.arkadipta.opus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException("Error processing OAuth2 user: " + ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        OAuth2UserInfo oauth2UserInfo = new OAuth2UserInfo(oauth2User.getAttributes());

        if (oauth2UserInfo.getEmail() == null || oauth2UserInfo.getEmail().isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmailOptional(oauth2UserInfo.getEmail());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            user = updateExistingUser(user, oauth2UserInfo);
        } else {
            user = registerNewUser(oauth2UserInfo);
        }

        return new CustomOAuth2User(oauth2User.getAttributes(), user);
    }

    private User registerNewUser(OAuth2UserInfo oauth2UserInfo) {
        User user = new User();
        user.setName(oauth2UserInfo.getName());
        user.setEmail(oauth2UserInfo.getEmail());
        user.setUserName(generateUsername(oauth2UserInfo.getEmail()));
        user.setRoles(List.of("USER"));
        user.setEmailVerified(true); // Google emails are pre-verified
        user.setOauth2Provider("google");
        user.setOauth2ProviderId(oauth2UserInfo.getId());
        user.setProfileImageUrl(oauth2UserInfo.getImageUrl());

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oauth2UserInfo) {
        existingUser.setName(oauth2UserInfo.getName());
        existingUser.setProfileImageUrl(oauth2UserInfo.getImageUrl());

        return userRepository.save(existingUser);
    }

    private String generateUsername(String email) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int counter = 1;

        while (userRepository.findByUserName(username) != null) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}
```

### 3. Custom OAuth2 User Implementation

```java
package org.arkadipta.opus.service;

import org.arkadipta.opus.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomOAuth2User implements OAuth2User {

    private Map<String, Object> attributes;
    private User user;

    public CustomOAuth2User(Map<String, Object> attributes, User user) {
        this.attributes = attributes;
        this.user = user;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return user.getUserName();
    }

    public User getUser() {
        return user;
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getFullName() {
        return user.getName();
    }
}
```

### 4. Update User Entity

Add OAuth2 fields to your User entity:

```java
package org.arkadipta.opus.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String userName;

    private List<String> roles;

    @Column(unique = true, nullable = false)
    private String email;

    private String password; // Optional for OAuth2 users

    private boolean emailVerified = false;

    // OAuth2 specific fields
    private String oauth2Provider; // "google", "facebook", etc.
    private String oauth2ProviderId; // Provider's user ID
    private String profileImageUrl;

    // Existing fields...
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Task> tasks;

    // Helper method to check if user is OAuth2 user
    public boolean isOAuth2User() {
        return oauth2Provider != null && !oauth2Provider.isEmpty();
    }
}
```

### 5. Update UserRepository

Add method to find user by email with Optional:

```java
package org.arkadipta.opus.repository;

import org.arkadipta.opus.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Existing methods
    User findByUserName(String userName);
    User findByEmail(String email);

    // New methods for OAuth2
    Optional<User> findByEmailOptional(String email);
    Optional<User> findByOauth2ProviderAndOauth2ProviderId(String provider, String providerId);

    // Check if user exists
    boolean existsByEmail(String email);
    boolean existsByUserName(String userName);
}
```

---

## Security Configuration

### OAuth2 Security Configuration

Create or update your security configuration to support OAuth2:

```java
package org.arkadipta.opus.config;

import org.arkadipta.opus.service.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

@Configuration
public class OAuth2SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    @Autowired
    private OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;

    @Bean
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/", "/error", "/favicon.ico",
                    "/oauth2/**", "/login/**",
                    "/public/**", "/auth/login",
                    "/auth/create-user"
                ).permitAll()

                // Protected endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")

                .anyRequest().authenticated()
            )
            // OAuth2 Login Configuration
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/oauth2/authorization/google")
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/oauth2/callback/*")
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oauth2AuthenticationSuccessHandler)
                .failureHandler(oauth2AuthenticationFailureHandler)
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CORS configuration (existing)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Your existing CORS configuration
        return null; // Implement based on your existing config
    }
}
```

### OAuth2 Success Handler

Handle successful OAuth2 authentication:

```java
package org.arkadipta.opus.config;

import org.arkadipta.opus.service.CustomOAuth2User;
import org.arkadipta.opus.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException {

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect.");
            return;
        }

        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();

        // Generate JWT token for OAuth2 user
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", oauth2User.getUser().getId());
        claims.put("email", oauth2User.getUser().getEmail());
        claims.put("roles", oauth2User.getUser().getRoles());
        claims.put("oauth2", true);

        String token = jwtUtil.generateToken(oauth2User.getName(), claims);
        String refreshToken = jwtUtil.generateRefreshToken(oauth2User.getName());

        // Redirect to frontend with tokens
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("token", token)
                .queryParam("refreshToken", refreshToken)
                .queryParam("username", oauth2User.getName())
                .queryParam("email", oauth2User.getEmail())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
```

### OAuth2 Failure Handler

Handle OAuth2 authentication failures:

```java
package org.arkadipta.opus.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                      HttpServletResponse response,
                                      AuthenticationException exception) throws IOException {

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("error", "authentication_failed")
                .queryParam("message", exception.getLocalizedMessage())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
```

---

## Frontend Integration

### HTML Login Page

Create a simple HTML page for testing OAuth2 login:

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Opus Login</title>
    <style>
      body {
        font-family: Arial, sans-serif;
        max-width: 400px;
        margin: 100px auto;
        padding: 20px;
        text-align: center;
      }
      .login-container {
        border: 1px solid #ddd;
        border-radius: 8px;
        padding: 30px;
        background-color: #f9f9f9;
      }
      .google-login-btn {
        background-color: #4285f4;
        color: white;
        padding: 12px 24px;
        border: none;
        border-radius: 4px;
        font-size: 16px;
        cursor: pointer;
        text-decoration: none;
        display: inline-block;
        margin: 10px;
      }
      .google-login-btn:hover {
        background-color: #357ae8;
      }
      .or-divider {
        margin: 20px 0;
        color: #666;
      }
      .traditional-login {
        border-top: 1px solid #ddd;
        padding-top: 20px;
        margin-top: 20px;
      }
    </style>
  </head>
  <body>
    <div class="login-container">
      <h2>Welcome to Opus</h2>
      <p>Please sign in to continue</p>

      <!-- OAuth2 Google Login -->
      <a href="/oauth2/authorization/google" class="google-login-btn">
        🔐 Sign in with Google
      </a>

      <div class="or-divider">OR</div>

      <!-- Traditional Login Form -->
      <div class="traditional-login">
        <form action="/auth/user-varification/login" method="post">
          <input type="text" name="username" placeholder="Username" required />
          <input
            type="password"
            name="password"
            placeholder="Password"
            required
          />
          <button type="submit">Sign In</button>
        </form>
      </div>
    </div>
  </body>
</html>
```

### JavaScript Frontend Integration

For React/Vue/Angular applications:

```javascript
// OAuth2 Login
const loginWithGoogle = () => {
  window.location.href = "http://localhost:8080/oauth2/authorization/google";
};

// Handle OAuth2 Redirect
const handleOAuth2Redirect = () => {
  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get("token");
  const refreshToken = urlParams.get("refreshToken");
  const error = urlParams.get("error");

  if (error) {
    console.error("OAuth2 authentication failed:", error);
    // Handle error (show message, redirect to login, etc.)
    return;
  }

  if (token) {
    // Store tokens
    localStorage.setItem("token", token);
    localStorage.setItem("refreshToken", refreshToken);

    // Redirect to dashboard or home page
    window.location.href = "/dashboard";
  }
};

// API calls with token
const makeAuthenticatedRequest = async (url, options = {}) => {
  const token = localStorage.getItem("token");

  const defaultOptions = {
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
      ...options.headers,
    },
  };

  return fetch(url, { ...options, ...defaultOptions });
};
```

### React Component Example

```jsx
import React, { useEffect } from "react";

const OAuth2RedirectHandler = () => {
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get("token");
    const refreshToken = urlParams.get("refreshToken");
    const error = urlParams.get("error");

    if (error) {
      console.error("OAuth2 Error:", error);
      // Redirect to login with error message
      window.location.href = "/login?error=" + error;
      return;
    }

    if (token && refreshToken) {
      localStorage.setItem("token", token);
      localStorage.setItem("refreshToken", refreshToken);

      // Redirect to dashboard
      window.location.href = "/dashboard";
    }
  }, []);

  return (
    <div>
      <h2>Processing authentication...</h2>
    </div>
  );
};

const LoginPage = () => {
  const handleGoogleLogin = () => {
    window.location.href = "http://localhost:8080/oauth2/authorization/google";
  };

  return (
    <div>
      <h2>Login to Opus</h2>
      <button onClick={handleGoogleLogin}>Sign in with Google</button>
    </div>
  );
};

export { OAuth2RedirectHandler, LoginPage };
```

---

## Testing the Implementation

### Manual Testing Steps

#### 1. Test OAuth2 Login Flow

1. **Start your Spring Boot application**

   ```bash
   mvn spring-boot:run
   ```

2. **Navigate to login page**

   ```
   http://localhost:8080/oauth2/authorization/google
   ```

3. **Expected Flow:**
   - Redirects to Google login
   - User logs in with Google account
   - User grants permissions
   - Redirects back to your app
   - JWT token generated
   - User authenticated

#### 2. Test API Endpoints

```bash
# Test protected endpoint with OAuth2-generated token
curl -X GET http://localhost:8080/user/profile \
  -H "Authorization: Bearer <oauth2-generated-token>"

# Test admin endpoint (if user has admin role)
curl -X GET http://localhost:8080/admin/users \
  -H "Authorization: Bearer <oauth2-generated-token>"
```

#### 3. Database Verification

Check your database to ensure OAuth2 users are created:

```sql
SELECT id, name, email, username, oauth2_provider, oauth2_provider_id, email_verified
FROM users
WHERE oauth2_provider = 'google';
```

### Automated Testing

#### Unit Tests

```java
package org.arkadipta.opus.service;

import org.arkadipta.opus.entity.User;
import org.arkadipta.opus.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    void testLoadUser_NewUser() {
        // Mock OAuth2UserRequest and OAuth2User
        OAuth2UserRequest userRequest = mock(OAuth2UserRequest.class);
        OAuth2User oauth2User = mock(OAuth2User.class);

        Map<String, Object> attributes = Map.of(
            "sub", "google123",
            "name", "John Doe",
            "email", "john@example.com",
            "picture", "https://example.com/photo.jpg"
        );

        when(oauth2User.getAttributes()).thenReturn(attributes);
        when(userRepository.findByEmailOptional("john@example.com"))
            .thenReturn(Optional.empty());
        when(userRepository.findByUserName(any())).thenReturn(null);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        // Test
        OAuth2User result = customOAuth2UserService.processOAuth2User(userRequest, oauth2User);

        // Verify
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }
}
```

#### Integration Tests

```java
package org.arkadipta.opus.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestMockMvc
class OAuth2IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testOAuth2AuthorizationEndpoint() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpected(redirectedUrlPattern("https://accounts.google.com/o/oauth2/auth**"));
    }
}
```

---

## Advanced Features

### 1. Multiple OAuth2 Providers

Add support for Facebook, GitHub, etc.:

```properties
# Facebook OAuth2
spring.security.oauth2.client.registration.facebook.client-id=YOUR_FACEBOOK_APP_ID
spring.security.oauth2.client.registration.facebook.client-secret=YOUR_FACEBOOK_APP_SECRET
spring.security.oauth2.client.registration.facebook.scope=email,public_profile

# GitHub OAuth2
spring.security.oauth2.client.registration.github.client-id=YOUR_GITHUB_CLIENT_ID
spring.security.oauth2.client.registration.github.client-secret=YOUR_GITHUB_CLIENT_SECRET
spring.security.oauth2.client.registration.github.scope=user:email
```

### 2. Custom User Info Mapping

```java
@Service
public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId,
                                                   Map<String, Object> attributes) {
        switch (registrationId.toLowerCase()) {
            case "google":
                return new GoogleOAuth2UserInfo(attributes);
            case "facebook":
                return new FacebookOAuth2UserInfo(attributes);
            case "github":
                return new GitHubOAuth2UserInfo(attributes);
            default:
                throw new OAuth2AuthenticationProcessingException(
                    "Login with " + registrationId + " is not supported");
        }
    }
}
```

### 3. Account Linking

Allow users to link multiple OAuth2 accounts:

```java
@RestController
@RequestMapping("/api/oauth2")
public class OAuth2AccountController {

    @PostMapping("/link/{provider}")
    public ResponseEntity<?> linkAccount(@PathVariable String provider,
                                       Principal principal) {
        // Generate state parameter for security
        String state = generateState(principal.getName(), provider);

        String authUrl = "/oauth2/authorization/" + provider + "?state=" + state;

        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    @PostMapping("/unlink/{provider}")
    public ResponseEntity<?> unlinkAccount(@PathVariable String provider,
                                         Principal principal) {
        // Remove OAuth2 provider association
        userService.unlinkOAuth2Provider(principal.getName(), provider);

        return ResponseEntity.ok(Map.of("message", "Account unlinked successfully"));
    }
}
```

### 4. Custom Scopes and Permissions

Request additional permissions from Google:

```properties
# Request additional Google scopes
spring.security.oauth2.client.registration.google.scope=openid,profile,email,https://www.googleapis.com/auth/drive.readonly,https://www.googleapis.com/auth/calendar.readonly
```

Handle additional user data:

```java
@Service
public class GoogleApiService {

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    public String getUserDriveFiles(String username) {
        OAuth2AuthorizedClient client = authorizedClientService
            .loadAuthorizedClient("google", username);

        if (client == null) {
            throw new IllegalStateException("User not authorized with Google");
        }

        String accessToken = client.getAccessToken().getTokenValue();

        // Use access token to call Google Drive API
        WebClient webClient = WebClient.builder()
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .build();

        return webClient.get()
            .uri("https://www.googleapis.com/drive/v3/files")
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }
}
```

### 5. Token Refresh for OAuth2

```java
@Component
public class OAuth2TokenRefreshService {

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Scheduled(fixedRate = 3600000) // Check every hour
    public void refreshExpiredTokens() {
        // Get all OAuth2 users
        List<User> oauth2Users = userRepository.findByOauth2ProviderIsNotNull();

        for (User user : oauth2Users) {
            OAuth2AuthorizedClient client = authorizedClientService
                .loadAuthorizedClient(user.getOauth2Provider(), user.getUserName());

            if (client != null && isTokenExpiringSoon(client.getAccessToken())) {
                // Token will be automatically refreshed by Spring Security
                logger.info("Refreshing token for user: " + user.getUserName());
            }
        }
    }

    private boolean isTokenExpiringSoon(OAuth2AccessToken token) {
        if (token.getExpiresAt() == null) {
            return false;
        }

        Instant expirationTime = token.getExpiresAt();
        Instant now = Instant.now();
        Duration timeUntilExpiration = Duration.between(now, expirationTime);

        return timeUntilExpiration.toMinutes() < 30; // Refresh if expires in 30 minutes
    }
}
```

---

## Production Considerations

### 1. Security Best Practices

#### Secure Configuration

```properties
# Production configuration
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}

# Use HTTPS in production
spring.security.oauth2.client.registration.google.redirect-uri=https://yourdomain.com/login/oauth2/code/google

# Secure cookies
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=strict
```

#### CSRF Protection

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/**") // Only for API endpoints
            );

        return http.build();
    }
}
```

### 2. Database Optimization

#### Indexes for OAuth2 Queries

```sql
-- Add indexes for OAuth2-related queries
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_oauth2_provider ON users(oauth2_provider);
CREATE INDEX idx_users_oauth2_provider_id ON users(oauth2_provider_id);
CREATE INDEX idx_users_oauth2_composite ON users(oauth2_provider, oauth2_provider_id);
```

#### Data Cleanup

```java
@Service
public class OAuth2DataCleanupService {

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupExpiredOAuth2Sessions() {
        // Remove expired OAuth2 authorized clients
        oauth2AuthorizedClientService.removeExpiredClients();

        // Clean up orphaned OAuth2 data
        userRepository.cleanupOrphanedOAuth2Data();
    }
}
```

### 3. Monitoring and Logging

```java
@Component
public class OAuth2EventListener {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2EventListener.class);

    @EventListener
    public void handleAuthenticationSuccess(OAuth2AuthenticationSuccessEvent event) {
        OAuth2User user = (OAuth2User) event.getAuthentication().getPrincipal();
        logger.info("OAuth2 login successful for user: {}", user.getAttribute("email"));

        // Send metrics to monitoring system
        meterRegistry.counter("oauth2.login.success", "provider", "google").increment();
    }

    @EventListener
    public void handleAuthenticationFailure(OAuth2AuthenticationFailureEvent event) {
        logger.warn("OAuth2 login failed: {}", event.getException().getMessage());

        // Send metrics to monitoring system
        meterRegistry.counter("oauth2.login.failure", "provider", "google").increment();
    }
}
```

### 4. Rate Limiting

```java
@Component
public class OAuth2RateLimitingFilter implements Filter {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                        FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (httpRequest.getRequestURI().startsWith("/oauth2/")) {
            String clientIp = getClientIp(httpRequest);
            String key = "oauth2_rate_limit:" + clientIp;

            String attempts = redisTemplate.opsForValue().get(key);
            if (attempts != null && Integer.parseInt(attempts) > 5) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(429); // Too Many Requests
                return;
            }

            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, Duration.ofMinutes(15));
        }

        chain.doFilter(request, response);
    }
}
```

---

## Troubleshooting

### Common Issues and Solutions

#### 1. "redirect_uri_mismatch" Error

**Problem**: Google returns an error about redirect URI mismatch.

**Solution**:

```
1. Check Google Cloud Console OAuth2 client configuration
2. Ensure redirect URIs match exactly:
   - Development: http://localhost:8080/login/oauth2/code/google
   - Production: https://yourdomain.com/login/oauth2/code/google
3. No trailing slashes or extra parameters
4. Use exact same protocol (http vs https)
```

#### 2. "invalid_client" Error

**Problem**: Google returns invalid client error.

**Solution**:

```
1. Verify Client ID and Client Secret are correct
2. Check environment variables are loaded properly
3. Ensure OAuth2 client is enabled in Google Cloud Console
4. Check if client secret has been regenerated
```

#### 3. User Information Not Retrieved

**Problem**: OAuth2 login works but user info is empty.

**Solution**:

```java
// Check scopes in application.properties
spring.security.oauth2.client.registration.google.scope=openid,profile,email

// Verify user info endpoint
spring.security.oauth2.client.provider.google.user-info-uri=https://openidconnect.googleapis.com/v1/userinfo
spring.security.oauth2.client.provider.google.user-name-attribute=sub
```

#### 4. Session/Cookie Issues

**Problem**: Users get logged out immediately or sessions don't persist.

**Solution**:

```properties
# Session configuration
server.servlet.session.timeout=30m
server.servlet.session.cookie.max-age=1800
server.servlet.session.cookie.http-only=true

# For development (HTTP)
server.servlet.session.cookie.secure=false

# For production (HTTPS)
server.servlet.session.cookie.secure=true
```

#### 5. CORS Issues with Frontend

**Problem**: Frontend can't communicate with OAuth2 endpoints.

**Solution**:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(Arrays.asList(
        "http://localhost:3000",
        "https://yourdomain.com"
    ));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### Debugging Tips

#### 1. Enable Debug Logging

```properties
# OAuth2 debug logging
logging.level.org.springframework.security.oauth2=DEBUG
logging.level.org.springframework.security.web=DEBUG
logging.level.org.arkadipta.opus.service.CustomOAuth2UserService=DEBUG

# HTTP client debug
logging.level.org.springframework.web.client.RestTemplate=DEBUG
```

#### 2. Test OAuth2 Flow Manually

```bash
# Step 1: Get authorization URL
curl -X GET "http://localhost:8080/oauth2/authorization/google" -v

# Step 2: Check redirect URI in browser network tab

# Step 3: Verify token generation in logs
```

#### 3. Validate JWT Tokens

Use jwt.io to decode and validate generated tokens:

```
1. Copy token from OAuth2 response
2. Go to https://jwt.io
3. Paste token in debugger
4. Verify claims and signature
```

---

## Summary

This comprehensive guide has covered:

1. **OAuth2 Theory** - Understanding the concepts and flow
2. **Google Cloud Setup** - Configuring OAuth2 credentials
3. **Spring Boot Implementation** - Complete code examples
4. **Security Configuration** - Proper security setup
5. **Frontend Integration** - How to integrate with web apps
6. **Testing** - Manual and automated testing approaches
7. **Advanced Features** - Multiple providers, account linking
8. **Production Deployment** - Security and optimization
9. **Troubleshooting** - Common issues and solutions

### Next Steps

1. **Implement basic OAuth2 flow** following the guide
2. **Test with Google authentication** in development
3. **Add frontend integration** for your specific use case
4. **Configure for production** with proper security measures
5. **Consider additional providers** if needed
6. **Implement advanced features** based on requirements

### Key Benefits Achieved

- ✅ Secure authentication without password storage
- ✅ Rich user profile data from Google
- ✅ Seamless user experience with "Sign in with Google"
- ✅ JWT token integration with existing system
- ✅ Production-ready security configuration
- ✅ Comprehensive error handling and monitoring

Your Opus application now supports modern OAuth2 authentication alongside traditional login methods!
