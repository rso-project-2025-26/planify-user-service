package com.planify.user_service.repository;

import com.planify.user_service.model.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {
    Optional<OrganizationEntity> findBySlug(String slug);

    List<OrganizationEntity> findByCreatedByUserId(UUID userId);
}
