package com.planify.user_service.service;

import com.planify.user_service.model.*;
import com.planify.user_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final OrganizationMembershipRepository organizationMembershipRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final AuthService authService;

    /**
     * Ustvari novo organizacijo in dodeli glavnega uporabnika vlogo ORG_ADMIN.
     */
    @Transactional
    public OrganizationEntity createOrganization(Organization organization) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(organization.getEmail());
        req.setUsername(organization.getSlug() + "_admin");
        req.setPassword(organization.getPassword());
        req.setFirstName("Administrator");
        req.setLastName(organization.getName());
        req.setRole("org_admin");
        Map<String, Object> response = authService.registerUser(req);
        UserEntity userCreated = (UserEntity) response.get("user");

        OrganizationEntity org = new OrganizationEntity();
        org.setName(organization.getName());
        org.setSlug(organization.getSlug());
        org.setDescription(organization.getDescription());
        org.setType(organization.getBusiness() ? OrganizationType.BUSINESS : OrganizationType.PERSONAL);
        org.setCreatedByUserId(userCreated.getId());
        org.setCreatedAt(LocalDateTime.now());

        OrganizationEntity savedOrg = organizationRepository.save(org);

        // Dodaj ustvarjalca kot ORG_ADMIN
        OrganizationMembershipEntity membership = new OrganizationMembershipEntity();
        membership.setUser(userCreated);
        membership.setOrganization(savedOrg);
        membership.setRole(OrganizationRole.ORG_ADMIN);
        membership.setCreatedAt(LocalDateTime.now());

        membershipRepository.save(membership);

        log.info("Organization {} created by user {}", savedOrg.getId(), userCreated.getId());
        return savedOrg;
    }

    public List<OrganizationMembershipEntity> getOrganizationUsers(UUID orgId) {
        return membershipRepository.findByOrganizationId(orgId);
    }

    public Optional<OrganizationRole> getUserRoleInOrganization(UUID userId, UUID orgId) {
        return membershipRepository.findByUserIdAndOrganizationId(userId, orgId)
                .map(OrganizationMembershipEntity::getRole);
    }

    private OrganizationEntity getOrganization(UUID orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
    }

    private UserEntity getUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private OrganizationMembershipEntity getMembership(UUID orgId, UUID userId) {
        return membershipRepository.findByUserIdAndOrganizationId(userId, orgId)
                .orElseThrow(() -> new RuntimeException("Membership not found"));
    }

    public List<JoinRequestEntity> getJoinRequests(UUID orgId) {
        return joinRequestRepository.findByOrganizationIdAndStatus(orgId, JoinRequestStatus.PENDING);
    }

    @Transactional
    public void removeUserFromOrganization(UUID orgId, UUID userId, UUID requestedByUserId) {
        // Preveri, da ima requestedByUserId vloga ORG_ADMIN
        OrganizationRole requestorRole = getUserRoleInOrganization(requestedByUserId, orgId)
                .orElseThrow(() -> new RuntimeException("No access"));

        if (!requestorRole.equals(OrganizationRole.ORG_ADMIN)) {
            throw new RuntimeException("Only administartor can remove members");
        }

        OrganizationMembershipEntity membership = getMembership(orgId, userId);

        if (userId == requestedByUserId) {
            throw new RuntimeException("You cannot remove yourself from organization.");
        }

        membershipRepository.delete(membership);
        log.info("User {} removed from organization {}", userId, orgId);
    }

    @Transactional
    public InvitationEntity inviteUserToOrganization(UUID orgId, UUID userId, UUID requestedByUserId, OrganizationRole role) {
        // Preveri, da ima requestedByUserId vloga ORG_ADMIN
        OrganizationRole requestorRole = getUserRoleInOrganization(requestedByUserId, orgId)
                .orElseThrow(() -> new RuntimeException("No access"));

        if (!requestorRole.equals(OrganizationRole.ORG_ADMIN)) {
            throw new RuntimeException("Only administrator can add new members");
        }

        OrganizationEntity org = getOrganization(orgId);
        UserEntity invitedUser = getUser(userId);

        // Ne moremo povabiti uporabnika, če povabilo že obstaja
        List<InvitationEntity> pendingInvites =
                invitationRepository.findByOrganizationIdAndStatusAndUserId(orgId, InvitationStatus.PENDING, userId);

        if (!pendingInvites.isEmpty()) {
            throw new RuntimeException("User already has a pending invitation");
        }

        // Nemoremo povabiti uporabnika, ki je že poslal join request
        joinRequestRepository.findByUserIdAndOrganizationId(userId, orgId)
                .filter(jr -> jr.getStatus() == JoinRequestStatus.PENDING)
                .ifPresent(jr -> {
                    throw new RuntimeException("User already has a pending join request");
                });

        // Ne moremo povabiti uporabnika, ki je že del organizacije
        Optional<OrganizationMembershipEntity> inOrganization =
                organizationMembershipRepository.findByUserIdAndOrganizationId(userId, orgId);

        if (inOrganization.isPresent()) {
            throw new RuntimeException("User is already member of the organization");
        }

        InvitationEntity invitation = new InvitationEntity();
        invitation.setOrganization(org);
        invitation.setUser(invitedUser);
        invitation.setRole(role != null ? role : OrganizationRole.GUEST);
        invitation.setToken(UUID.randomUUID().toString().replace("-", ""));
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setCreatedAt(LocalDateTime.now());
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        invitation.setCreatedByUserId(requestedByUserId);

        InvitationEntity saved = invitationRepository.save(invitation);
        log.info("User {} invited to organization {} by {}", userId, orgId, requestedByUserId);
        return saved;
    }

    @Transactional
    public void approveJoinRequest(UUID orgId, UUID userId, UUID joinRequestId) {
        // Preveri, da ima requestedByUserId vloga ORG_ADMIN
        OrganizationRole requestorRole = getUserRoleInOrganization(userId, orgId)
                .orElseThrow(() -> new RuntimeException("No access"));

        if (!requestorRole.equals(OrganizationRole.ORG_ADMIN)) {
            throw new RuntimeException("Only administrator can add new members");
        }

        JoinRequestEntity joinRequest = joinRequestRepository.findById(joinRequestId)
            .orElseThrow(() -> new RuntimeException("Join request not found"));

        if (!joinRequest.getOrganization().getId().equals(orgId)) {
            throw new RuntimeException("Join request does not belong to this organization");
        }

        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw new RuntimeException("Join request is not pending");
        }

        UserEntity requestByUser = joinRequest.getUser();
        OrganizationEntity org = joinRequest.getOrganization();

        // Preverimo ali je uporabnik že del organizacije
        membershipRepository.findByUserIdAndOrganizationId(requestByUser.getId(), orgId)
                .ifPresent(m -> {
                    throw new RuntimeException("User is already a member of this organization");
                });

        // Ustvarimo membership in uporabniku dodelimo vlogo GUSET
        OrganizationMembershipEntity membership = new OrganizationMembershipEntity();
        membership.setUser(joinRequest.getUser());
        membership.setOrganization(org);
        membership.setRole(OrganizationRole.GUEST);
        membership.setCreatedAt(LocalDateTime.now());
        membership.setUpdatedAt(null);

        membershipRepository.save(membership);

        // Posodobimo status join request-a
        joinRequest.setStatus(JoinRequestStatus.APPROVED);
        joinRequest.setHandledAt(LocalDateTime.now());
        joinRequest.setHandledByUserId(userId);
        joinRequestRepository.save(joinRequest);

        log.info("Join request {} approved by {} for user {} in org {}",
                joinRequestId, userId, requestByUser.getId(), orgId);
    }

    @Transactional
    public void rejectJoinRequest(UUID orgId, UUID joinRequestId, UUID userId) {
        // Preveri, da ima requestedByUserId vloga ORG_ADMIN
        OrganizationRole requestorRole = getUserRoleInOrganization(userId, orgId)
                .orElseThrow(() -> new RuntimeException("No access"));

        if (!requestorRole.equals(OrganizationRole.ORG_ADMIN)) {
            throw new RuntimeException("Only administrator can add new members");
        }

        JoinRequestEntity joinRequest = joinRequestRepository.findById(joinRequestId)
                .orElseThrow(() -> new RuntimeException("Join request not found"));

        if (!joinRequest.getOrganization().getId().equals(orgId)) {
            throw new RuntimeException("Join request does not belong to this organization");
        }

        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw new RuntimeException("Join request is not pending");
        }

        joinRequest.setStatus(JoinRequestStatus.REJECTED);
        joinRequest.setHandledAt(LocalDateTime.now());
        joinRequest.setHandledByUserId(userId);
        joinRequestRepository.save(joinRequest);

        log.info("Join request {} rejected by {} for user {} in org {}",
                joinRequestId, userId, joinRequest.getUser().getId(), orgId);
    }

    @Transactional
    public void changeUserRole(UUID orgId,
                               UUID targetUserId,
                               OrganizationRole newRole,
                               UUID requestedByUserId) {
        if (newRole == null) {
            throw new RuntimeException("New role must not be null");
        }

        // Preveri, da ima requestedByUserId vloga ORG_ADMIN
        OrganizationRole requestorRole = getUserRoleInOrganization(requestedByUserId, orgId)
                .orElseThrow(() -> new RuntimeException("No access"));

        if (!requestorRole.equals(OrganizationRole.ORG_ADMIN)) {
            throw new RuntimeException("Only administrator can add new members");
        }

        OrganizationMembershipEntity membership = getMembership(orgId, targetUserId);

        if (membership.getRole() == newRole) {
            return; // Nič ne naredimo
        }

        membership.setRole(newRole);
        membership.setUpdatedAt(LocalDateTime.now());
        membershipRepository.save(membership);

        log.info("Role of user {} in organization {} changed to {} by {}",
                targetUserId, orgId, newRole, requestedByUserId);
    }
}
