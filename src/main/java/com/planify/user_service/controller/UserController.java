package com.planify.user_service.controller;

import com.planify.user_service.model.JoinRequestEntity;
import com.planify.user_service.model.UserEntity;
import com.planify.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Pridobimo vse uporabnike v naši bazi
     * @return seznam uporabnikov
     */
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping
    public ResponseEntity<List<UserEntity>> getUsers() {
        List<UserEntity> users = userService.getUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Pridobimo seznam uporabnikov znotraj organizacije
     * @param orgId: Id organizacije, za katero želimo pridobiti uporabnike
     * @return seznam uporabnika določene organizacije
     */
    @PreAuthorize("@orgSecurity.isAdmin(#orgId, authentication)")
    @GetMapping("{orgId}/users")
    public ResponseEntity<List<UserEntity>> getOrganizationsUsers(@PathVariable("orgId") UUID orgId) {
        List<UserEntity> users = userService.getUsersOfOrganization(orgId).stream().toList();
        return ResponseEntity.ok(users);
    }

    /**
     * Pridobimo trenutno prijavljenega uporabnika
     * @return objekt uporabnika
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserEntity> getCurrentUser() {
        UserEntity user = userService.getCurrentUser();
        return ResponseEntity.ok(user);
    }

    /**
     * Izbriše trenutno prijavljenega uporabnika
     * @return
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser() {
        UserEntity user = userService.getCurrentUser();
        UUID userId = user.getId();
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Izvozimo podatke trenutno prijavljenega uporabnika
     * @return preslikavo lastnost vrednost (vseh podatkov o uporabniku)
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/export")
    public ResponseEntity<Map<String, Object>> exportUserData() {
        UserEntity user = userService.getCurrentUser();

        Map<String, Object> data = userService.exportUserData(user.getId());
        return ResponseEntity.ok(data);
    }

    /**
     * Pošlje zahtevo za vstop v organizacijo
     * @param orgId; id organizacije, v katero hočemo vstopiti
     * @return vrne kreiran objekt zahteve za pristop
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{orgId}/join-request")
    public ResponseEntity<JoinRequestEntity> sendJoinRequest(
            @PathVariable UUID orgId) {
        UserEntity user = userService.getCurrentUser();

        JoinRequestEntity request = userService.sendJoinRequest(orgId, user.getId());
        return ResponseEntity.ok(request);
    }

}
