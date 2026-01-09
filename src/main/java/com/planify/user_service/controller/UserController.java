package com.planify.user_service.controller;

import com.planify.user_service.model.JoinRequestEntity;
import com.planify.user_service.model.OrganizationEntity;
import com.planify.user_service.model.UserEntity;
import com.planify.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Users", description = "User management and profile endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

    private final UserService userService;

    /**
     * Pridobimo vse uporabnike v naši bazi
     * @return seznam uporabnikov
     */
    @Operation(
            summary = "Get system users",
            description = "Get list of all users in the application. Only visible to application administrators."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List successfully retrieved"),
            @ApiResponse(responseCode = "500", description = "Error occurred while retrieving the list"),
            @ApiResponse(responseCode = "401", description = "Logged in user is not an application administrator")
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
            summary = "Get user by ID",
            description = "Get user data in the application by their ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User successfully retrieved"),
            @ApiResponse(responseCode = "500", description = "Error occurred while retrieving user"),
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserEntity> getUser(
            @Parameter(required = true)
            @PathVariable UUID userId) {
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
            summary = "Get user by search value",
            description = "Get list of all users in the application whose username starts with the search value. Only visible to organization administrators."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List successfully retrieved"),
            @ApiResponse(responseCode = "500", description = "Error occurred while retrieving the list"),
            @ApiResponse(responseCode = "401", description = "Logged in user is not an organization administrator")
    })
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<List<UserEntity>> searchUsers(
            @Parameter(required = true)
            @RequestParam String username) {
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
            summary = "Get organizations where user is a member",
            description = "Get list of all organizations where the currently logged in user is a member."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List successfully retrieved"),
            @ApiResponse(responseCode = "500", description = "Error occurred while retrieving the list"),
            @ApiResponse(responseCode = "401", description = "Logged in user is not authenticated")
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
            summary = "Get unprocessed sent requests of currently logged in user",
            description = "Get list of sent requests still awaiting response."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List successfully retrieved"),
            @ApiResponse(responseCode = "500", description = "Error occurred while retrieving the list"),
            @ApiResponse(responseCode = "401", description = "Logged in user is not authenticated")
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
            summary = "Get current user",
            description = "Get object of currently logged in user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User successfully retrieved"),
            @ApiResponse(responseCode = "500", description = "Error occurred while retrieving user"),
            @ApiResponse(responseCode = "401", description = "User is not logged in")
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
            summary = "Delete current user",
            description = "Delete currently logged in user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User successfully deleted"),
            @ApiResponse(responseCode = "500", description = "Error occurred while deleting user"),
            @ApiResponse(responseCode = "401", description = "User is not logged in")
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
            summary = "Get current user",
            description = "Export data of currently logged in user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data successfully exported"),
            @ApiResponse(responseCode = "500", description = "Error occurred during export"),
            @ApiResponse(responseCode = "401", description = "User is not logged in")
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
            summary = "Send request to join organization",
            description = "User sends request to join a specific organization."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Request successfully sent"),
            @ApiResponse(responseCode = "500", description = "Error occurred while sending request"),
            @ApiResponse(responseCode = "401", description = "User is not logged in")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{orgId}/join-request")
    public ResponseEntity<JoinRequestEntity> sendJoinRequest(
            @Parameter(required = true)
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
