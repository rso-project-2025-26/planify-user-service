package com.planify.user_service.model;

public enum OrganizationRole {
    GUEST,           // Lahko vidi dogodke organizacije
    ORGANISER,       // Lahko ustvari in ureja dogodke
    ORG_ADMIN        // Admin organizacije + organiser
}
