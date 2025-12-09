package com.planify.user_service.repository;

import com.planify.user_service.model.KeycloakRole;
import com.planify.user_service.model.OrganizationMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembershipEntity, UUID> {
    List<OrganizationMembershipEntity> findByUserId(UUID userId);

    List<OrganizationMembershipEntity> findByOrganizationId(UUID organizationId);

    List<OrganizationMembershipEntity> findByUserIdAndOrganizationId(UUID userId, UUID orgId);
    Optional<OrganizationMembershipEntity> findByUserIdAndOrganizationIdAndRole(UUID userId, UUID orgId, KeycloakRole role);
}
