package com.planify.user_service.model.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvitationRespondedEvent(
        UUID invitationId,
        String eventType,              // DECLINED / ACCEPTED
        List<String> adminIds,
        UUID organizationId,
        String organizationName,
        UUID invitedUserId,
        String invitedUsername,
        Instant occurredAt
) {}