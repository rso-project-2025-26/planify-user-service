package com.planify.user_service.service;

import com.planify.user_service.model.*;
import com.planify.user_service.repository.InvitationRepository;
import com.planify.user_service.repository.OrganizationMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationsService {

    private final InvitationRepository invitationRepository;
    private final OrganizationMembershipRepository membershipRepository;

    private final UserService userService;

    public List<InvitationEntity> getInvitations() {
        return invitationRepository.findAll();
    }

    public List<InvitationEntity> getInvitationsByUserId(UUID userId) {
        return invitationRepository.findByUserId(userId);
    }

    public List<InvitationEntity> getInvitationsByCurrentUserId() {
        UUID userId = userService.getCurrentUser().getId();
        return invitationRepository.findByUserId(userId);
    }

    @Transactional
    public InvitationEntity acceptInvitation(String invitationToken) {
        UserEntity user = userService.getCurrentUser();
        // Preveri, da je povabilo z invitationId dejansko za tega uporabnika
        InvitationEntity invitation = invitationRepository.findByToken(invitationToken)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));


        if (invitation.getUser() == null || !user.getId().equals(invitation.getUser().getId())) {
            throw new RuntimeException("This invitation is not for you!");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("This invitation is expired!");
        }

        OrganizationEntity org = invitation.getOrganization();

        // Dodamo uporabnika v organizacijo
        OrganizationMembershipEntity membership = new OrganizationMembershipEntity();
        membership.setUser(user);
        membership.setOrganization(org);
        membership.setRole(invitation.getRole());
        membership.setCreatedAt(LocalDateTime.now());
        membership.setUpdatedAt(null);

        membershipRepository.save(membership);

        // Posodobimo status povabila
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        log.info("Invitation {} accepted by {} in org {}",
                invitation.getId(), user.getId(), org.getId());

        return invitation;
    }

    @Transactional
    public void declineInvitation(String invitationToken) {
        UserEntity user = userService.getCurrentUser();
        // Preveri, da je povabilo z invitationId dejansko za tega uporabnika
        InvitationEntity invitation = invitationRepository.findByToken(invitationToken)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (invitation.getUser() == null || !user.getId().equals(invitation.getUser().getId())) {
            throw new RuntimeException("This invitation is not for you!");
        }

        invitationRepository.delete(invitation);

        log.info("Invitation {} declined by {}",
                invitation.getId(), user.getId());
    }
}
