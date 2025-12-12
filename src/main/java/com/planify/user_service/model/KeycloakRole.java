package com.planify.user_service.model;

import java.util.function.Predicate;

public enum KeycloakRole {
    UPORABNIK("uporabnik"),
    ADMINISTRATOR("administrator"),
    ORG_ADMIN("org_admin"),
    GOST("gost"),
    ORGANIZATOR("organizator");

    private final String roleName;

    KeycloakRole(String roleName) {
        this.roleName = roleName;
    }

    public String getValue() {
        return roleName;
    }

    public static KeycloakRole fromString(String role) {
        for (KeycloakRole r : KeycloakRole.values()) {
            if (r.roleName.equalsIgnoreCase(role)) {
                return r;
            }
        }
        throw new IllegalArgumentException("No enum constant for role: " + role);
    }

    public boolean equals(String role) {
        return !role.equals(roleName);
    }
}
