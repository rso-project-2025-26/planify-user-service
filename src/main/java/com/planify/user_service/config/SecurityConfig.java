package com.planify.user_service.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@EnableMethodSecurity
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/auth/**").permitAll() // registracija, login
                .requestMatchers("/api/users/**").permitAll() // uporabniki
                .requestMatchers("/api/organizations/**").permitAll() // organizacije
                .requestMatchers("/internal/**").permitAll() // interni API (zaenkrat)
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/resilience/**").permitAll() // Fault tolerance monitoring
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/v3/api-docs",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api-docs/**",
                        "/api-docs",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/user-service/v3/api-docs/**",
                        "/user-service/v3/api-docs",
                        "/user-service/swagger-ui/**",
                        "/user-service/swagger-ui.html",
                        "/user-service/api-docs/**",
                        "/user-service/api-docs",
                        "/user-service/swagger-resources/**",
                        "/user-service/webjars/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        
        return http.build();
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        // Preberi vloge iz "roles" claia v JWT-ju
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtConverter());
        return converter;
    }
}
