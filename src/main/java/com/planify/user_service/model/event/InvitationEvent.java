package com.planify.user_service.model.event;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvitationEvent(
        String eventType,              // SENT / EXPIRED / DECLINED / ACCEPTED
        UUID invitationId,
        UUID orgId,
        String organizationSlug,
        String organizationName,
        LocalDateTime expiresAt,
        UUID invitedUserId,
        String invitedUsername,
        Instant occurredAt
) {}