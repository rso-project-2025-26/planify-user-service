package com.planify.user_service.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KeycloakHealthIndicator implements HealthIndicator {

    private final Environment environment;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Health health() {

        try {
            // pridobimo vrednosti za keycloak
            String keycloakUrl = environment.getProperty("keycloak.url");
            String realm = environment.getProperty("keycloak.realm");
            String adminUsername = environment.getProperty("keycloak.admin.username");
            String adminPassword = environment.getProperty("keycloak.admin.password");
            String tokenUrl = keycloakUrl + "/realms/master/protocol/openid-connect/token";

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", "admin-cli");
            body.add("username", adminUsername);
            body.add("password", adminPassword);
            body.add("grant_type", "password");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request =
                    new HttpEntity<>(body, headers);

            restTemplate.postForObject(tokenUrl, request, Map.class);

            return Health.up().withDetail("keycloak", "Available").build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("keycloak", "Unavailable")
                    .withException(e)
                    .build();
        }
    }
}
