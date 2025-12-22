package com.planify.user_service.model;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UserRoles {
    private UUID userId;
    private String firstName;
    private String lastName;
    private String username;
    private List<KeycloakRole> roles;
}
