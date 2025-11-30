package com.planify.user_service.model.event;

import java.time.Instant;
import java.util.UUID;

public record JoinRequestEvent(
        String eventType,              // SENT / APPROVED / REJECTED
        UUID joinRequestId,
        UUID organizationId,
        String organizationName,
        UUID requesterUserId,
        String requesterUsername,
        Instant occurredAt
) {}