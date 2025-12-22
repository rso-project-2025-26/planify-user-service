package com.planify.user_service.model.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvitationSentEvent(
        UUID invitationId,
        UUID organizationId,
        String organizationName,
        UUID invitedUserId,
        String invitedFirstName,
        String invitedLastName,
        String invitedEmail,
        Instant occurredAt
) {}