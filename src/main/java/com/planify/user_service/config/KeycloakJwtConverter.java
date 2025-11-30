package com.planify.user_service.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.*;
import java.util.stream.Collectors;

public class KeycloakJwtConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String RESOURCE_ACCESS = "resource_access";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Vloge iz Realm-a (globalne Keycloak vloge)
        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS);
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> realmRoles) {
            realmRoles.forEach(roleObj -> {
                String role = String.valueOf(roleObj);
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            });
        }

        // Client vloge (vloge dodeljene specifičnim klientom v Keycloak)
        Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS);
        if (resourceAccess != null) {
            resourceAccess.forEach((client, accessData) -> {
                if (accessData instanceof Map<?, ?> accessMap) {
                    Object clientRolesObj = accessMap.get("roles");

                    if (clientRolesObj instanceof Collection<?> clientRoles) {
                        clientRoles.forEach(roleObj -> {
                            String role = String.valueOf(roleObj);
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                        });
                    }
                }
            });
        }

        return authorities;
    }
}
