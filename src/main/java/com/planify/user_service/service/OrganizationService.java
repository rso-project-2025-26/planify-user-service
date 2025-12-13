package com.planify.user_service.service;

import com.planify.user_service.event.KafkaProducer;
import com.planify.user_service.model.*;
import com.planify.user_service.model.event.InvitationEvent;
import com.planify.user_service.model.event.JoinRequestEvent;
import com.planify.user_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    private final KafkaProducer kafkaProducer;

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
        membership.setRole(KeycloakRole.ORG_ADMIN);
        membership.setCreatedAt(LocalDateTime.now());

        membershipRepository.save(membership);

        log.info("Organization {} created by user {}", savedOrg.getId(), userCreated.getId());
        return savedOrg;
    }

    public List<OrganizationMembershipEntity> getOrganizationUsers(UUID orgId) {
        return membershipRepository.findByOrganizationId(orgId);
    }

    public List<KeycloakRole> getUserRolesInOrganization(UUID userId, UUID orgId) {
        return membershipRepository.findByUserIdAndOrganizationId(userId, orgId)
                .stream()
                .map(OrganizationMembershipEntity::getRole)
                .toList();
    }

    private OrganizationEntity getOrganization(UUID orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
    }

    private UserEntity getUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<OrganizationMembershipEntity> getMembership(UUID orgId, UUID userId) {
        return membershipRepository.findByUserIdAndOrganizationId(userId, orgId);
    }

    public List<JoinRequestEntity> getJoinRequests(UUID orgId) {
        return joinRequestRepository.findByOrganizationIdAndStatus(orgId, JoinRequestStatus.PENDING);
    }

    public OrganizationSummary getOrganizationByAdmin(UUID adminId) {
        return organizationMembershipRepository.findOrganizationByAdmin(adminId).orElseThrow(() -> new RuntimeException("User is not admin of any organization"));
    }

    public List<UserRoles> getUsersAndRoles(UUID orgId) {
        List<OrganizationMembershipEntity> memberships =
                organizationMembershipRepository.findByOrganizationId(orgId);

        Map<UUID, UserRoles> byUserId = new LinkedHashMap<>();

        for (OrganizationMembershipEntity membership : memberships) {
            UserEntity user = membership.getUser();
            UUID userId = user.getId();

            UserRoles userRoles = byUserId.computeIfAbsent(userId, id -> {
                UserRoles ur = new UserRoles();
                ur.setUserId(id);
                ur.setUsername(user.getUsername());
                ur.setFirstName(user.getFirstName());
                ur.setLastName(user.getLastName());
                ur.setRoles(new ArrayList<>());
                return ur;
            });

            KeycloakRole role = membership.getRole();
            if (role != null && !userRoles.getRoles().contains(role)) {
                userRoles.getRoles().add(role);
            }
        }

        return new ArrayList<>(byUserId.values());
    }

    public List<OrganizationEntity> searchOrgs(String serachValue) {
        return organizationRepository.findOrgsBySearchValue(serachValue);
    }

    @Transactional
    public void removeUserFromOrganization(UUID orgId, UUID userId, UUID requestedByUserId) {
        // Preveri, da ima requestedByUserId vloga ORG_ADMIN
        if (!isUserOrgAdmin(orgId, requestedByUserId)) {
            throw new RuntimeException("Only administartor can remove members");
        }

        List<OrganizationMembershipEntity> membership = getMembership(orgId, userId);

        if (userId == requestedByUserId) {
            throw new RuntimeException("You cannot remove yourself from organization.");
        }

        membershipRepository.deleteAllById(membership.stream().map(OrganizationMembershipEntity::getId).toList());
        // Preveri ali ima uporabnik, katero od vlog v katerikoli drugi organizaciji
        List<OrganizationMembershipEntity> userMemberships = membershipRepository.findByUserId(userId);
        for (OrganizationMembershipEntity m : membership) {
            long cnt = userMemberships.stream()
                    .filter(um -> um.getRole().equals(m.getRole()) && !um.getOrganization().getId().equals(orgId))
                    .count();
            if (cnt == 0) {
                // Če nima v nobeni drugi, odstrani to vlogo iz Keycloaka
                UserEntity user = getUser(userId);
                authService.removeRole(user.getKeycloakId(), m.getRole());
            }
        }
        log.info("User {} removed from organization {}", userId, orgId);
    }

    @Transactional
    public void removeMyselfFromOrganization(UUID orgId, UUID userId) {
        List<OrganizationMembershipEntity> membership = getMembership(orgId, userId);

        membershipRepository.deleteAllById(membership.stream().map(OrganizationMembershipEntity::getId).toList());
        // Preveri ali ima uporabnik, katero od vlog v katerikoli drugi organizaciji
        List<OrganizationMembershipEntity> userMemberships = membershipRepository.findByUserId(userId);
        for (OrganizationMembershipEntity m : membership) {
            long cnt = userMemberships.stream()
                    .filter(um -> um.getRole().equals(m.getRole()) && !um.getOrganization().getId().equals(orgId))
                    .count();
            if (cnt == 0) {
                // Če nima v nobeni drugi, odstrani to vlogo iz Keycloaka
                UserEntity user = getUser(userId);
                authService.removeRole(user.getKeycloakId(), m.getRole());
            }
        }
        log.info("User {} removed from organization {}", userId, orgId);
    }

    @Transactional
    public InvitationEntity inviteUserToOrganization(UUID orgId, UUID userId, UUID requestedByUserId, KeycloakRole role) {
        // Preveri, da ima requestedByUserId vloga ORG_ADMIN
        if (!isUserOrgAdmin(orgId, requestedByUserId)) {
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
        List<JoinRequestEntity> requests = joinRequestRepository.findByUserIdAndOrganizationId(userId, orgId)
                .stream()
                .filter(jr -> jr.getStatus() == JoinRequestStatus.PENDING).toList();
        if (!requests.isEmpty()) {
            throw new RuntimeException("User already has a pending join request");
        }

        // Ne moremo povabiti uporabnika, ki je že del organizacije
        Optional<OrganizationMembershipEntity> inOrganization =
                organizationMembershipRepository.findByUserIdAndOrganizationIdAndRole(userId, orgId, role);

        if (inOrganization.isPresent()) {
            throw new RuntimeException("User already has role " + role.getValue() + " in the organization");
        }

        InvitationEntity invitation = new InvitationEntity();
        invitation.setOrganization(org);
        invitation.setUser(invitedUser);
        invitation.setRole(role != null ? role : KeycloakRole.GUEST);
        invitation.setToken(UUID.randomUUID().toString().replace("-", ""));
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setCreatedAt(LocalDateTime.now());
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        invitation.setCreatedByUserId(requestedByUserId);

        InvitationEntity saved = invitationRepository.save(invitation);

        // Sproižimo Kafka dogodek, da je poslano novo vabilo v organizacijo
        var event = new InvitationEvent(
                "SENT",
                invitation.getId(),
                orgId,
                org.getSlug(),
                org.getName(),
                invitation.getExpiresAt(),
                userId,
                invitedUser.getUsername(),
                Instant.now()
        );
        kafkaProducer.publishInvitationEvent(event);

        log.info("User {} invited to organization {} by {}", userId, orgId, requestedByUserId);
        return saved;
    }

    @Transactional
    public void approveJoinRequest(UUID orgId, UUID userId, UUID joinRequestId) {
        // Preveri, da ima requestedByUserId vloga ORG_ADMIN
        if (!isUserOrgAdmin(orgId, userId)) {
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
        if (!membershipRepository.findByUserIdAndOrganizationId(requestByUser.getId(), orgId).isEmpty()) {
            throw new RuntimeException("User is already a member of the organization");
        }

        // Ustvarimo membership in uporabniku dodelimo vlogo GOST
        OrganizationMembershipEntity membership = new OrganizationMembershipEntity();
        membership.setUser(joinRequest.getUser());
        membership.setOrganization(org);
        membership.setRole(KeycloakRole.GUEST);
        membership.setCreatedAt(LocalDateTime.now());

        membershipRepository.save(membership);

        UserEntity user = getUser(requestByUser.getId());
        authService.assignRole(user.getKeycloakId(), KeycloakRole.GUEST);

        // Posodobimo status join request-a
        joinRequest.setStatus(JoinRequestStatus.APPROVED);
        joinRequest.setHandledAt(LocalDateTime.now());
        joinRequest.setHandledByUserId(userId);
        joinRequestRepository.save(joinRequest);

        // Kafka event ob privolitvi zahteve
        var event = new JoinRequestEvent(
                "APPROVED",
                joinRequest.getId(),
                orgId,
                org.getName(),
                requestByUser.getId(),
                requestByUser.getUsername(),
                Instant.now()
        );
        kafkaProducer.publishJoinRequestEvent(event);

        log.info("Join request {} approved by {} for user {} in org {}",
                joinRequestId, userId, requestByUser.getId(), orgId);
    }

    @Transactional
    public void rejectJoinRequest(UUID orgId, UUID joinRequestId, UUID userId) {
        // Preveri, da ima requestedByUserId vloga ORG_ADMIN
        if (!isUserOrgAdmin(orgId, userId)) {
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

        // Kafka event ob zavrnitvi zahteve
        var event = new JoinRequestEvent(
                "REJECTED",
                joinRequest.getId(),
                orgId,
                joinRequest.getOrganization().getName(),
                joinRequest.getUser().getId(),
                joinRequest.getUser().getUsername(),
                Instant.now()
        );

        log.info("Join request {} rejected by {} for user {} in org {}",
                joinRequestId, userId, joinRequest.getUser().getId(), orgId);
    }

    public void changeUserRoles(UUID orgId,
                               UUID targetUserId,
                               List<KeycloakRole> newRoles,
                               UUID requestedByUserId) {
        removeUserFromOrganization(orgId, targetUserId, requestedByUserId);

        for (KeycloakRole newRole: newRoles) {
            changeUserRole(orgId, targetUserId, newRole, requestedByUserId);
        }
    }

    @Transactional
    public void changeUserRole(UUID orgId,
                               UUID targetUserId,
                               KeycloakRole newRole,
                               UUID requestedByUserId) {
        if (newRole == null) {
            log.error("Given role is null.");
            throw new RuntimeException("New role must not be null");
        }

        if (!isUserOrgAdmin(orgId, requestedByUserId)) {
            log.error("User {} is not administrator of the organization {} and thus it can't change users roles.", requestedByUserId, orgId);
            throw new RuntimeException("Only administrator can add new members");
        }

        // Preverimo ali je uporabnik že admin, kakšen organizacije
        // Uporabnik je lahko admin le ene organizacije na enkrat
        if (newRole == KeycloakRole.ORG_ADMIN) {
            Optional<OrganizationSummary> targetOrgId = membershipRepository.findOrganizationByAdmin(targetUserId);
            if (targetOrgId.isPresent() && !targetOrgId.get().getId().equals(orgId)) {
                log.error("User {} is already admin of organization {}.", targetUserId, targetOrgId);
                throw new RuntimeException("User is already admin of another organization.");
            }
        }

        UserEntity targetUser = getUser(targetUserId);
        OrganizationEntity org = getOrganization(orgId);
        OrganizationMembershipEntity newMembership = new OrganizationMembershipEntity();
        newMembership.setUser(targetUser);
        newMembership.setOrganization(org);
        newMembership.setRole(newRole);
        newMembership.setCreatedAt(LocalDateTime.now());

        membershipRepository.save(newMembership);


        authService.assignRole(targetUser.getKeycloakId(), newRole);

        log.info("Role of user {} in organization {} changed to {} by {}",
                targetUserId, orgId, newRole, requestedByUserId);
    }

    public boolean isUserOrgAdmin(UUID orgId, UUID userId) {
        List<KeycloakRole> role = getUserRolesInOrganization(userId, orgId);

        return role.stream().anyMatch(m -> m.equals(KeycloakRole.ORG_ADMIN));
    }
}
