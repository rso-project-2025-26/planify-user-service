package com.planify.user_service.repository;

import com.planify.user_service.model.KeycloakRole;
import com.planify.user_service.model.OrganizationEntity;
import com.planify.user_service.model.OrganizationMembershipEntity;
import com.planify.user_service.model.OrganizationSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembershipEntity, UUID> {
    List<OrganizationMembershipEntity> findByUserId(UUID userId);

    List<OrganizationMembershipEntity> findByOrganizationId(UUID organizationId);
    List<OrganizationMembershipEntity> findByOrganizationIdAndRole(UUID organizationid, KeycloakRole role);

    List<OrganizationMembershipEntity> findByUserIdAndOrganizationId(UUID userId, UUID orgId);
    Optional<OrganizationMembershipEntity> findByUserIdAndOrganizationIdAndRole(UUID userId, UUID orgId, KeycloakRole role);

    @Query("""
        SELECT new com.planify.user_service.model.OrganizationSummary(
            om.organization.name,
            om.organization.slug,
            om.organization.id
        )
          FROM OrganizationMembershipEntity om
         WHERE om.user.id = :adminId
           AND UPPER(om.role) = 'ORG_ADMIN'
    """)
    Optional<OrganizationSummary> findOrganizationByAdmin(UUID adminId);
}
