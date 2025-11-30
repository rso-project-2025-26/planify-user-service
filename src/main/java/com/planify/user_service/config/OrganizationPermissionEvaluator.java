package com.planify.user_service.config;

import com.planify.user_service.model.OrganizationMembershipEntity;
import com.planify.user_service.repository.OrganizationMembershipRepository;
import com.planify.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component("orgSecurity")
@RequiredArgsConstructor
public class OrganizationPermissionEvaluator {
    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    // Metoda se uporablja, za določanje dostopov do endpointov v aplikaciji,
    // saj so ti omejeni glede na vlogo in organizacijo
    public boolean hasRole(UUID orgId, String role, Authentication auth) {
        if (auth == null || orgId == null || role == null) return false;

        String keycloakId = auth.getName();
        UUID userId = userRepository.findUserIdByKeycloakId(keycloakId);

        Optional<OrganizationMembershipEntity> membershipEntity = membershipRepository
                .findByUserIdAndOrganizationId(userId, orgId);

        return membershipEntity
                .map(m -> m.getRole().name().equals(role))
                .orElse(false);
    }

    public boolean isAdmin(UUID orgId, Authentication auth) {
        return hasRole(orgId, "ORG_ADMIN", auth);
    }

    public boolean isOrganizer(UUID orgId, Authentication auth) {
        return hasRole(orgId, "ORGANIZER", auth);
    }

    public boolean isMember(UUID orgId, Authentication auth) {
        if (auth == null || orgId == null) return false;

        UUID userId = UUID.fromString(auth.getName());

        return membershipRepository
                .findByUserIdAndOrganizationId(userId, orgId)
                .isPresent();
    }
}
