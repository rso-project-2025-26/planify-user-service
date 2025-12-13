package com.planify.user_service.repository;

import com.planify.user_service.model.JoinRequestEntity;
import com.planify.user_service.model.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JoinRequestRepository extends JpaRepository<JoinRequestEntity, UUID> {
    List<JoinRequestEntity> findByOrganizationIdAndStatus(UUID orgId, JoinRequestStatus status);

    List<JoinRequestEntity> findByUserIdAndOrganizationId(UUID userId, UUID orgId);

    List<JoinRequestEntity> findByUserIdAndStatus(UUID userId, JoinRequestStatus ststus);
}