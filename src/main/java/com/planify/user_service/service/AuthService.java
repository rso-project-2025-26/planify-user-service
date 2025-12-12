package com.planify.user_service.service;

import com.planify.user_service.model.*;
import com.planify.user_service.repository.OrganizationMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final RestTemplate restTemplate = new RestTemplate();

    private final String keycloakUrl = "http://localhost:9080";
    private final String realm = "planify";
    private final String clientId = "planify-frontend";
    private final String clientSecret = "YOUR_CLIENT_SECRET";
    private final OrganizationMembershipRepository organizationMembershipRepository;

    public Map<String, Object> registerUser(RegisterRequest request) {

        // Ustvarimo novega uporabnika v Keycloak-u
        String keycloakUserId = createKeycloakUser(request);

        // Shranimo uporabnika v naš DB
        UserEntity user = userService.createLocalUserProfile(
                keycloakUserId,
                request.getEmail(),
                request.getUsername(),
                request.getFirstName(),
                request.getLastName()
        );

        // Vračamo profil prijavljenega uporabnika
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registration successful");
        response.put("user", user);
        return response;
    }

    private String createKeycloakUser(RegisterRequest req) {

        try {
            String url = keycloakUrl + "/admin/realms/" + realm + "/users";

            // Admin token (da lahko ustvarimo uporabnika)
            String adminToken = getAdminToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Ustvarimo Keycloak uporabnika
            Map<String, Object> kcUser = new HashMap<>();
            kcUser.put("username", req.getUsername());
            kcUser.put("email", req.getEmail());
            kcUser.put("firstName", req.getFirstName());
            kcUser.put("lastName", req.getLastName());
            kcUser.put("enabled", true);

            HttpEntity<?> entity = new HttpEntity<>(kcUser, headers);

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to create user in Keycloak");
            }

            String location = resp.getHeaders().getLocation().toString();
            String keycloakUserId = location.substring(location.lastIndexOf("/") + 1);

            // Uporabniku nastavimo geslo
            String pwdUrl = keycloakUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId + "/reset-password";

            HttpHeaders pwdHeaders = new HttpHeaders();
            pwdHeaders.setBearerAuth(adminToken);
            pwdHeaders.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> cred = new HashMap<>();
            cred.put("type", "password");
            cred.put("value", req.getPassword());
            cred.put("temporary", false);

            HttpEntity<?> pwdEntity = new HttpEntity<>(cred, pwdHeaders);

            restTemplate.put(pwdUrl, pwdEntity);

            // Nastavimo, da je email verified
            String updateUrl = keycloakUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId;

            Map<String, Object> updates = new HashMap<>();
            updates.put("emailVerified", true);
            updates.put("enabled", true);

            restTemplate.put(updateUrl, new HttpEntity<>(updates, pwdHeaders));

            KeycloakRole role = req.getRole() != null ? KeycloakRole.fromString(req.getRole()) : KeycloakRole.UPORABNIK;

            // Mu dodelimo keycloak vlogo UPORABNIK
            assignRoleToUser(keycloakUserId, role, adminToken);
            assignRoleToUser(keycloakUserId, KeycloakRole.UPORABNIK, adminToken);

            return keycloakUserId;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user in Keycloak. " + e.getMessage());
        }
    }

    private void assignRoleToUser(String userId, KeycloakRole roleName, String adminToken) {
        String normalized = roleName.toString().toLowerCase();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Pridobimo vloga iz Keycloaka
        String roleUrl = keycloakUrl + "/admin/realms/" + realm + "/roles/" + normalized;

        ResponseEntity<Map> roleResp = restTemplate.exchange(roleUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        Map<String, Object> roleRepresentation = roleResp.getBody();


        // Dodelimo vlogo uporabniku
        String assignUrl =
                keycloakUrl + "/admin/realms/" + realm +
                        "/users/" + userId + "/role-mappings/realm";

        List<Map<String, Object>> roles = new ArrayList<>();
        roles.add(roleRepresentation);

        restTemplate.postForEntity(assignUrl, new HttpEntity<>(roles, headers), String.class);
    }

    private String getAdminToken() {
        try {
            String url = keycloakUrl + "/realms/master/protocol/openid-connect/token";

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", "admin-cli");
            body.add("grant_type", "password");
            body.add("username", "admin");
            body.add("password", "admin");

            HttpEntity<?> entity = new HttpEntity<>(body, new HttpHeaders());

            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            return (String) response.get("access_token");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get admin token. " + e.getMessage());
        }
    }

    public void assignRole(String userKeycloakId, KeycloakRole role) {
        assignRoleToUser(userKeycloakId, role, getAdminToken());
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

    public void removeRole(String userKeycloakId, KeycloakRole role) {
        try {
            String adminToken = getAdminToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Pridobimo vloga iz Keycloaka
            String roleUrl = keycloakUrl + "/admin/realms/" + realm + "/roles/" + role.toString().toLowerCase();

            ResponseEntity<Map> roleResp = restTemplate.exchange(roleUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            Map<String, Object> roleRepresentation = roleResp.getBody();

            // Odstranimo vlogo uporabniku
            String removeUrl =
                    keycloakUrl + "/admin/realms/" + realm +
                            "/users/" + userKeycloakId + "/role-mappings/realm";

            List<Map<String, Object>> roles = new ArrayList<>();
            roles.add(roleRepresentation);

            restTemplate.exchange(removeUrl, HttpMethod.DELETE, new HttpEntity<>(roles, headers), String.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove role from user in Keycloak. " + e.getMessage());
        }
    }

}
