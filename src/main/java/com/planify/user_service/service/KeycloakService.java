package com.planify.user_service.service;

import com.planify.user_service.model.KeycloakRole;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class KeycloakService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.url:http://localhost:9080}")
    private String keycloakUrl;

    @Value("${keycloak.realm:planify}")
    private String realm;

    @Retry(name = "keycloakService")
    @CircuitBreaker(name = "keycloakService", fallbackMethod = "getAdminTokenFallback")
    public String getAdminToken() {
        log.debug("Fetching admin token from Keycloak");
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
            log.error("Failed to get admin token: {}", e.getMessage());
            throw new RuntimeException("Failed to get admin token. " + e.getMessage());
        }
    }

    private String getAdminTokenFallback(Exception ex) {
        log.error("Keycloak admin token unavailable. Fallback triggered. Error: {}", ex.getMessage());
        throw new RuntimeException("Keycloak authentication service is temporarily unavailable. Please try again later.");
    }

    @Retry(name = "keycloakService")
    @Bulkhead(name = "keycloakService")
    @CircuitBreaker(name = "keycloakService", fallbackMethod = "createUserFallback")
    public UUID createUser(String username, String email, String firstName, String lastName, String adminToken) {
        log.info("Creating Keycloak user for username: {}", username);
        try {
            String url = keycloakUrl + "/admin/realms/" + realm + "/users";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> kcUser = new HashMap<>();
            kcUser.put("username", username);
            kcUser.put("email", email);
            kcUser.put("firstName", firstName);
            kcUser.put("lastName", lastName);
            kcUser.put("enabled", true);

            HttpEntity<?> entity = new HttpEntity<>(kcUser, headers);

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to create user in Keycloak");
            }

            String location = resp.getHeaders().getLocation().toString();
            UUID keycloakUserId = UUID.fromString(location.substring(location.lastIndexOf("/") + 1));
            
            log.info("Successfully created Keycloak user with ID: {}", keycloakUserId);
            return keycloakUserId;
        } catch (Exception e) {
            log.error("Failed to create Keycloak user {}: {}", username, e.getMessage());
            throw new RuntimeException("Failed to create user in Keycloak. " + e.getMessage());
        }
    }

    private UUID createUserFallback(String username, String email, String firstName, String lastName, String adminToken, Exception ex) {
        log.error("Keycloak service unavailable. Fallback triggered for user: {}. Error: {}", username, ex.getMessage());
        throw new RuntimeException("Keycloak service temporarily unavailable. Please try again later.");
    }


    @Retry(name = "keycloakService")
    @CircuitBreaker(name = "keycloakService", fallbackMethod = "setPasswordFallback")
    public void setPassword(UUID keycloakUserId, String password, String adminToken) {
        log.debug("Setting password for user {}", keycloakUserId);
        try {
            String pwdUrl = keycloakUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId + "/reset-password";

            HttpHeaders pwdHeaders = new HttpHeaders();
            pwdHeaders.setBearerAuth(adminToken);
            pwdHeaders.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> cred = new HashMap<>();
            cred.put("type", "password");
            cred.put("value", password);
            cred.put("temporary", false);

            HttpEntity<?> pwdEntity = new HttpEntity<>(cred, pwdHeaders);

            restTemplate.put(pwdUrl, pwdEntity);
            log.debug("Password set successfully for user {}", keycloakUserId);
        } catch (Exception e) {
            log.error("Failed to set password for user {}: {}", keycloakUserId, e.getMessage());
            throw new RuntimeException("Failed to set password. " + e.getMessage());
        }
    }

    private void setPasswordFallback(UUID keycloakUserId, String password, String adminToken, Exception ex) {
        log.error("Failed to set password for user {}. Error: {}", keycloakUserId, ex.getMessage());
        throw new RuntimeException("Failed to set password. Keycloak service temporarily unavailable.");
    }


    @Retry(name = "keycloakService")
    @CircuitBreaker(name = "keycloakService", fallbackMethod = "updateUserFallback")
    public void updateUser(UUID keycloakUserId, Map<String, Object> updates, String adminToken) {
        log.debug("Updating user {} with attributes: {}", keycloakUserId, updates);
        try {
            String updateUrl = keycloakUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.put(updateUrl, new HttpEntity<>(updates, headers));
            log.debug("User {} updated successfully", keycloakUserId);
        } catch (Exception e) {
            log.error("Failed to update user {}: {}", keycloakUserId, e.getMessage());
            throw new RuntimeException("Failed to update user. " + e.getMessage());
        }
    }

    private void updateUserFallback(UUID keycloakUserId, Map<String, Object> updates, String adminToken, Exception ex) {
        log.error("Failed to update user {}. Error: {}", keycloakUserId, ex.getMessage());
        throw new RuntimeException("Failed to update user. Keycloak service temporarily unavailable.");
    }


    @Retry(name = "keycloakService")
    @Bulkhead(name = "keycloakService")
    @CircuitBreaker(name = "keycloakService", fallbackMethod = "assignRoleFallback")
    public void assignRole(UUID userId, KeycloakRole roleName, String adminToken) {
        log.info("Assigning role {} to user {}", roleName, userId);
        try {
            String normalized = roleName.toString().toLowerCase();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Pridobimo role iz Keycloaka
            String roleUrl = keycloakUrl + "/admin/realms/" + realm + "/roles/" + normalized;

            ResponseEntity<Map> roleResp = restTemplate.exchange(roleUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            Map<String, Object> roleRepresentation = roleResp.getBody();

            // Dodelimo nove role uporabniku
            String assignUrl = keycloakUrl + "/admin/realms/" + realm +
                    "/users/" + userId + "/role-mappings/realm";

            List<Map<String, Object>> roles = new ArrayList<>();
            roles.add(roleRepresentation);

            restTemplate.postForEntity(assignUrl, new HttpEntity<>(roles, headers), String.class);
            log.info("Successfully assigned role {} to user {}", roleName, userId);
        } catch (Exception e) {
            log.error("Failed to assign role {} to user {}: {}", roleName, userId, e.getMessage());
            throw new RuntimeException("Failed to assign role. " + e.getMessage());
        }
    }

    private void assignRoleFallback(UUID userId, KeycloakRole roleName, String adminToken, Exception ex) {
        log.error("Failed to assign role to user in Keycloak. User: {}, Role: {}, Error: {}",
                userId, roleName, ex.getMessage());
        throw new RuntimeException("Failed to assign role. Keycloak service temporarily unavailable.");
    }


    @Retry(name = "keycloakService")
    @CircuitBreaker(name = "keycloakService", fallbackMethod = "removeRoleFallback")
    public void removeRole(UUID userId, KeycloakRole roleName, String adminToken) {
        log.info("Removing role {} from user {}", roleName, userId);
        try {
            String normalized = roleName.toString().toLowerCase();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Pridobimo role iz Keycloaka
            String roleUrl = keycloakUrl + "/admin/realms/" + realm + "/roles/" + normalized;

            ResponseEntity<Map> roleResp = restTemplate.exchange(roleUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            Map<String, Object> roleRepresentation = roleResp.getBody();

            // Odstranimo role uporabniku
            String removeUrl = keycloakUrl + "/admin/realms/" + realm +
                    "/users/" + userId + "/role-mappings/realm";

            List<Map<String, Object>> roles = new ArrayList<>();
            roles.add(roleRepresentation);

            restTemplate.exchange(removeUrl, HttpMethod.DELETE, new HttpEntity<>(roles, headers), String.class);
            log.info("Successfully removed role {} from user {}", roleName, userId);
        } catch (Exception e) {
            log.error("Failed to remove role {} from user {}: {}", roleName, userId, e.getMessage());
            throw new RuntimeException("Failed to remove role. " + e.getMessage());
        }
    }

    private void removeRoleFallback(UUID userId, KeycloakRole roleName, String adminToken, Exception ex) {
        log.error("Failed to remove role from user in Keycloak. User: {}, Role: {}, Error: {}",
                userId, roleName, ex.getMessage());
        throw new RuntimeException("Failed to remove role. Keycloak service temporarily unavailable.");
    }
}
