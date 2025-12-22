package com.planify.user_service.model.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JoinRequestsSentEvent(
        UUID joinRequestId,
        List<String> adminIds,
        UUID organizationId,
        String organizationName,
        UUID requesterUserId,
        String requesterUsername,
        Instant occurredAt
) {}
