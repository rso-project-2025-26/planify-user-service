package com.planify.user_service.controller;

import com.planify.user_service.model.RegisterRequest;
import com.planify.user_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(
            summary = "Registracija novega uporabnika",
            description = "Ustvari novega uporabnika v sistemu in keycloak-u in vrne autentifikacijske tokene."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uporabnik uspešno registriran"),
            @ApiResponse(responseCode = "400", description = "Podatki so neveljavni")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authService.registerUser(request));
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    @Operation(
            summary = "Pridobi vlogo uporabnika znotraj organizacije.",
            description = "Vrne vlogo, ki jo ima trenutno prijavljeni uporabnik v določeni organizaciji."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "uspešno vrne vlogo v obliki niza"),
            @ApiResponse(responseCode = "500", description = "Pri pridobivanju vloge je prišlo do napake")
    })
    @GetMapping("{orgId}/roles")
    public ResponseEntity<?> getRoles(@PathVariable UUID orgId) {
        try{
            List<String> role = authService.getRolesForOrganization(orgId);
            return ResponseEntity.ok(role);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}