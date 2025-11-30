package com.planify.user_service.controller;

import com.planify.user_service.model.RegisterRequest;
import com.planify.user_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registracija novega uporabnika
     * @param request: objekt, ki vsebuje vse podatke za registracijo naovega uporabnika, v naš sistem in Keycloak
     * @return objekt uporabnika iz našega sistema
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authService.registerUser(request));
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    @GetMapping("{orgId}/roles")
    public ResponseEntity<String> getRoles(@PathVariable UUID orgId) {
        try{
            String role = authService.getRolesForOrganiyation(orgId);
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}