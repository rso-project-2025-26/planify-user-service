package com.planify.user_service.repository;

import com.planify.user_service.model.InvitationEntity;
import com.planify.user_service.model.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<InvitationEntity, UUID> {
    Optional<InvitationEntity> findByToken(String token);

    List<InvitationEntity> findByUserId(UUID userId);
    List<InvitationEntity> findByUserIdAndStatus(UUID userId, InvitationStatus status);

    List<InvitationEntity> findByOrganizationIdAndStatus(UUID orgId, InvitationStatus status);
    List<InvitationEntity> findByOrganizationIdAndStatusAndUserId(UUID orgId, InvitationStatus status, UUID userId);
}