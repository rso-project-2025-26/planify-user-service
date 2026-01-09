package com.planify.user_service.service;

import com.planify.user_service.model.*;
import com.planify.user_service.repository.OrganizationMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserService userService;
    private final KeycloakService keycloakClient;
    private final OrganizationMembershipRepository organizationMembershipRepository;

    public Map<String, Object> registerUser(RegisterRequest request) {

        // Ustvarimo novega uporabnika v Keycloak-u
        UUID keycloakUserId = createKeycloakUser(request);

        // Shranimo uporabnika v naš DB
        UserEntity user = userService.createLocalUserProfile(
                keycloakUserId,
                request.getEmail(),
                request.getMobile(),
                request.getUsername(),
                request.getFirstName(),
                request.getLastName(),
                request.getConsentEmail(),
                request.getConsentSms()
        );

        // Vračamo profil prijavljenega uporabnika
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registration successful");
        response.put("user", user);
        return response;
    }

    private UUID createKeycloakUser(RegisterRequest req) {
        log.info("Creating Keycloak user for username: {}", req.getUsername());
        try {
            // Admin token (da lahko ustvarimo uporabnika)
            String adminToken = keycloakClient.getAdminToken();

            // Ustvarimo Keycloak uporabnika
            UUID keycloakUserId = keycloakClient.createUser(
                    req.getUsername(),
                    req.getEmail(),
                    req.getFirstName(),
                    req.getLastName(),
                    adminToken
            );

            // Uporabniku nastavimo geslo
            keycloakClient.setPassword(keycloakUserId, req.getPassword(), adminToken);

            // Nastavimo, da je email verified
            Map<String, Object> updates = new HashMap<>();
            updates.put("emailVerified", true);
            updates.put("enabled", true);
            keycloakClient.updateUser(keycloakUserId, updates, adminToken);

            // Določimo role
            KeycloakRole role = req.getRole() != null ? KeycloakRole.fromString(req.getRole()) : KeycloakRole.UPORABNIK;
            keycloakClient.assignRole(keycloakUserId, role, adminToken);
            keycloakClient.assignRole(keycloakUserId, KeycloakRole.UPORABNIK, adminToken);

            return keycloakUserId;
        } catch (Exception e) {
            log.error("Failed to create Keycloak user: {}", e.getMessage());
            throw new RuntimeException("Failed to create user in Keycloak. " + e.getMessage());
        }
    }

    public void assignRole(UUID userKeycloakId, KeycloakRole role) {
        String adminToken = keycloakClient.getAdminToken();
        keycloakClient.assignRole(userKeycloakId, role, adminToken);
    }

    public List<String> getRolesForOrganization(UUID orgId) {
        UserEntity user = userService.getCurrentUser();
        List<OrganizationMembershipEntity> membershipEntity = organizationMembershipRepository.findByUserIdAndOrganizationId(user.getId(), orgId);
        List<String> role = membershipEntity
                .stream()
                .map(m -> m.getRole().getValue())
                .toList();
        return role;
    }

    public void removeRole(UUID userKeycloakId, KeycloakRole role) {
        String adminToken = keycloakClient.getAdminToken();
        keycloakClient.removeRole(userKeycloakId, role, adminToken);
    }

}
