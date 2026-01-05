package com.planify.user_service.controller;

import com.planify.user_service.model.RegisterRequest;
import com.planify.user_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration and authentication endpoints")
public class AuthController {

    private final AuthService authService;

    /**
     * Registracija novega uporabnika
     * @param request: objekt, ki vsebuje vse podatke za registracijo naovega uporabnika, v naš sistem in Keycloak
     * @return objekt uporabnika iz našega sistema
     */
    @Operation(
        summary = "Register new user",
        description = "Creates a new user account in both the database and Keycloak. Assigns default role UPORABNIK."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User successfully registered"),
        @ApiResponse(responseCode = "400", description = "Invalid data - email already exists or validation failed", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Parameter(required = true)
            @RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authService.registerUser(request));
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    @Operation(
        summary = "Get user roles in organization",
        description = "Returns the roles that the authenticated user has within a specific organization (e.g., ORG_ADMIN, ORGANISER, MEMBER)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Roles retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping("{orgId}/roles")
    @PreAuthorize("hasRole('UPORABNIK')")
    public ResponseEntity<?> getRoles(
        @Parameter(required = true)
        @PathVariable UUID orgId) {
        try{
            List<String> role = authService.getRolesForOrganization(orgId);
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}