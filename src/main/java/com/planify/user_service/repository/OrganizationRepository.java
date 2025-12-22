package com.planify.user_service.repository;

import com.planify.user_service.model.OrganizationEntity;
import com.planify.user_service.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {
    Optional<OrganizationEntity> findBySlug(String slug);

    List<OrganizationEntity> findByCreatedByUserId(UUID userId);

    @Query("""
       SELECT o
       FROM OrganizationEntity o
       WHERE o.slug LIKE CONCAT(:searchValue, '%')
          OR o.name LIKE CONCAT(:searchValue, '%')
       """)
    List<OrganizationEntity> findOrgsBySearchValue(String searchValue);
}
