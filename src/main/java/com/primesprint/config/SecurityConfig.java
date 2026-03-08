package com.primesprint.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Allow CORS preflight
                    .requestMatchers("/auth/**").permitAll() // login/register open
                    .requestMatchers("/admin/**").hasRole("SENIOR")
                    .requestMatchers("/user/**").hasAnyRole("SENIOR","JUNIOR")
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // Swagger open
                    .anyRequest().authenticated()
                    ).exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                        response.setContentType("application/json");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" + authException.getMessage() + "\"}");
                }));
        return http.build();
    }
}
