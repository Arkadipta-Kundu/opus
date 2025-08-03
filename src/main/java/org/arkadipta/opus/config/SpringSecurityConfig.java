package org.arkadipta.opus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SpringSecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http.cors(cors -> cors.configurationSource(corsConfigurationSource())) // Add CORS
                                                                                                   // configuration
                        .csrf(AbstractHttpConfigurer::disable)
                        .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/public/**",
                                                                "/auth/user-varification/forget-password",
                                                                "/auth/user-varification/reset-password",
                                                                "/auth/create-user", // Fixed missing slash
                                                                "/auth/user-varification/login",
                                                                "/auth/user-varification/send", // Add OTP send endpoint
                                                                "/auth/user-varification/verify") // Add OTP verify
                                                                                                  // endpoint
                                                .permitAll()
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .anyRequest().authenticated()) // authentication & authorization
                                .httpBasic(Customizer.withDefaults());

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Allow requests from React frontend
                configuration.setAllowedOriginPatterns(Arrays.asList(
                                "http://localhost:3000",
                                "http://127.0.0.1:3000",
                                "https://*.netlify.app",
                                "https://*.vercel.app",
                                "https://*.herokuapp.com",
                                "https://*.railway.app"));

                // Allow all HTTP methods
                configuration.setAllowedMethods(Arrays.asList(
                                "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));

                // Allow all headers
                configuration.setAllowedHeaders(Arrays.asList("*"));

                // Expose headers that frontend might need
                configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

                // Allow credentials (important for authentication)
                configuration.setAllowCredentials(true);

                // Cache preflight requests for 1 hour
                configuration.setMaxAge(3600L);

                // Apply CORS configuration to all endpoints
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
