package com.planify.user_service.repository;

import com.planify.user_service.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByKeycloakId(UUID keycloakId);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
       SELECT u
       FROM UserEntity u
       WHERE u.username LIKE CONCAT(:searchValue, '%')
       """)
    List<UserEntity> findUsersBySearchValue(String searchValue);

    @Query("""
       SELECT u
       FROM UserEntity u
       JOIN OrganizationMembershipEntity m ON m.user.id = u.id
       WHERE m.organization.id = :orgId
       """)
    List<UserEntity> findUsersByOrganization(UUID orgId);

    @Query("""
       SELECT o
       FROM OrganizationEntity o
       JOIN OrganizationMembershipEntity m ON m.organization.id = o.id
       WHERE m.user.id = :userId
       """)
    List<OrganizationEntity> findOrganizationByUsers(UUID userId);

    @Query("""
        SELECT u.id
          FROM UserEntity u
         WHERE u.keycloakId = :keycloakId
    """)
    UUID findUserIdByKeycloakId(UUID keycloakId);

    @Query("""
        SELECT u
          FROM UserEntity u
         WHERE u.keycloakId = :keycloakId
           AND u.deletedAt is null
    """)
    Optional<UserEntity> findActiveByKeycloakId(UUID keycloakId);
}
