package com.planify.user_service.controller;

import com.planify.user_service.model.JoinRequestEntity;
import com.planify.user_service.model.OrganizationEntity;
import com.planify.user_service.model.UserEntity;
import com.planify.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Pridobimo vse uporabnike v naši bazi
     * @return seznam uporabnikov
     */
    @Operation(
            summary = "Pridobi uporabnike sistema",
            description = "Pridobi seznam vseh uporabnikv v aplikaciji. To lahko vidi le administrator aplikacije."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seznam je uspešno pridobljen"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri pridobivanju seznama"),
            @ApiResponse(responseCode = "403", description = "Prijavljeni uporabnik ni administrator aplikacije")
    })
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping
    public ResponseEntity<List<UserEntity>> getUsers() {
        try{
            List<UserEntity> users = userService.getUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Pridobimo uporabnika glede na keycloak id
     * @return Uporabnik
     */
    @Operation(
            summary = "Pridobi uporabnika glede na id",
            description = "Pridobi podatkr o uporabniku v aplikaciji glede na njegov id."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uporabnik je uspešno pridobljen"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri pridobivanju uporabnika"),
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserEntity> getUser(@PathVariable UUID userId) {
        try{
            UserEntity user = userService.getUser(userId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Pridobimo vse uporabnike v naši bazi glede na iskalno vrednost
     * @return seznam uporabnikov
     */
    @Operation(
            summary = "Pridobi uporabnike sistema",
            description = "Pridobi seznam vseh uporabnikv v aplikaciji, katerih username se začne z iskalno vrednostjo. To lahko vidi le administrator organizacije."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seznam je uspešno pridobljen"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri pridobivanju seznama"),
            @ApiResponse(responseCode = "403", description = "Prijavljeni uporabnik ni administrator organizacije")
    })
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<List<UserEntity>> searchUsers(@RequestParam String username) {
        try{
            List<UserEntity> users = userService.searchUsers(username);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }


    /**
     * Pridobimo seznam organizacij, katerih član je trenutni uporabnik
     * @return seznam organizacij trenutnega uporabnika
     */
    @Operation(
            summary = "Pridobi organizacije, katerih član je uporabnik",
            description = "Pridobi seznam vseh organizacij v katerih je trenutno prijavljen uporabnik član."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seznam je uspešno pridobljen"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri pridobivanju seznama"),
            @ApiResponse(responseCode = "403", description = "Prijavljeni uporabnik ni prijavljen v sistem")
    })
    @PreAuthorize("hasRole('UPORABNIK')")
    @GetMapping("me/orgs")
    public ResponseEntity<?> getUsersOrganizations() {
        try{
            List<OrganizationEntity> users = userService.getUsersOrganizations().stream().toList();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    /**
     * Pridobimo seznam poslanih prošenj za včalnitev v organizacije
     * @return seznam poslanih prošenj, ki še čakajo na odgovor
     */
    @Operation(
            summary = "Pridobi neobdelane poslane prošnje trenutno prijavljenega uporabnika",
            description = "Pridobi seznam oslanih prošenj, ki še čakajo na odgovor."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seznam je uspešno pridobljen"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri pridobivanju seznama"),
            @ApiResponse(responseCode = "403", description = "Prijavljeni uporabnik ni prijavljen v sistem")
    })
    @PreAuthorize("hasRole('UPORABNIK')")
    @GetMapping("me/join-requests")
    public ResponseEntity<?> getUsersJoinRequests() {
        try{
            List<JoinRequestEntity> users = userService.getPendingUsersJoinRequests().stream().toList();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    /**
     * Pridobimo trenutno prijavljenega uporabnika
     * @return objekt uporabnika
     */
    @Operation(
            summary = "Pridobi trenutnega uporabnika",
            description = "Pridobi objekt trenutno prijavljenega uporabnika."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uporabnik je uspešno pridobljen"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri pridobivanju uporabnika"),
            @ApiResponse(responseCode = "403", description = "Uporabnik ni prijavljen")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserEntity> getCurrentUser() {
        try{
            UserEntity user = userService.getCurrentUser();
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Izbriše trenutno prijavljenega uporabnika
     * @return
     */
    @Operation(
            summary = "Izbriše trenutnega uporabnika",
            description = "Izbriše  trenutno prijavljenega uporabnika."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Uporabnik je uspešno izbrisan"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri brisanju uporabnika"),
            @ApiResponse(responseCode = "403", description = "Uporabnik ni prijavljen")
    })
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser() {
        try{
            UserEntity user = userService.getCurrentUser();
            UUID userId = user.getId();
            userService.deleteUser(userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(null);
        }

    }

    /**
     * Izvozimo podatke trenutno prijavljenega uporabnika
     * @return preslikavo lastnost vrednost (vseh podatkov o uporabniku)
     */
    @Operation(
            summary = "Pridobi trenutnega uporabnika",
            description = "Izvozi podatke trenutno prijavljenega uporabnika."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Podatki so uspešno izvezeni"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri izvozu"),
            @ApiResponse(responseCode = "403", description = "Uporabnik ni prijavljen")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/export")
    public ResponseEntity<Map<String, Object>> exportUserData() {
        try {
            UserEntity user = userService.getCurrentUser();

            Map<String, Object> data = userService.exportUserData(user.getId());
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Pošlje zahtevo za vstop v organizacijo
     * @param orgId; id organizacije, v katero hočemo vstopiti
     * @return vrne kreiran objekt zahteve za pristop
     */
    @Operation(
            summary = "Pošlje prošnjo za vstop v oprganizacijo",
            description = "Uporabnik pošlje prošnjo za vstop v določeno organizacijo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pročnja je uspešno poslana"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri pošiljanju prošnje"),
            @ApiResponse(responseCode = "403", description = "Uporabnik ni prijavljen")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{orgId}/join-request")
    public ResponseEntity<JoinRequestEntity> sendJoinRequest(
            @PathVariable UUID orgId) {
        try{
            UserEntity user = userService.getCurrentUser();

            JoinRequestEntity request = userService.sendJoinRequest(orgId, user.getId());
            return ResponseEntity.status(201).body(request);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

}
