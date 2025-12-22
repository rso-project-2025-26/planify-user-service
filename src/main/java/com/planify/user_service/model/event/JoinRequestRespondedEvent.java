package com.planify.user_service.model.event;

import java.time.Instant;
import java.util.UUID;

public record JoinRequestRespondedEvent(
        String eventType,              // APPROVED / REJECTED
        UUID joinRequestId,
        UUID organizationId,
        String organizationName,
        UUID requesterUserId,
        String requesterFirstName,
        String requesterLastName,
        String requesterEmail,
        Instant occurredAt
) {}