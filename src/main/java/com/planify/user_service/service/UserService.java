package com.planify.user_service.service;

import com.planify.user_service.event.KafkaProducer;
import com.planify.user_service.model.*;
import com.planify.user_service.model.event.JoinRequestRespondedEvent;
import com.planify.user_service.model.event.JoinRequestsSentEvent;
import com.planify.user_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final InvitationRepository invitationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final JoinRequestRepository joinRequestRepository;

    private final KafkaProducer kafkaProducer;


    /**
     * Sinhronizira uporabnika iz Keycloak tokena.
     * Ob prvem klicu: ustvari UserEntity z vlogo GUEST.
     */
    @Transactional
    public UserEntity syncUserFromToken() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");

        Optional<UserEntity> existingUser = userRepository.findByKeycloakId(keycloakId);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // Ustvari novega uporabnika
        UserEntity newUser = new UserEntity();
        newUser.setKeycloakId(keycloakId);
        newUser.setEmail(email);
        newUser.setFirstName(firstName);
        newUser.setFirstName(lastName);
        newUser.setCreatedAt(LocalDateTime.now());

        return userRepository.save(newUser);
    }

    @Transactional
    public UserEntity getCurrentUser() {
        return syncUserFromToken(); // Avtomatsko sinhronizira ob branju
    }

    @Transactional
    public void deleteUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Soft delete
        user.setDeletedAt(LocalDateTime.now());

        // Izbriši ali anonimiziraj membership-e
        membershipRepository.deleteAll(user.getMemberships());

        userRepository.save(user);
        log.info("User {} marked as deleted", userId);
    }

    public Map<String, Object> exportUserData(UUID userId) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("memberships", user.getMemberships());

        return data;
    }

    public List<UserEntity> getUsers() {
        return userRepository.findAll();
    }

    public List<UserEntity> searchUsers(String serachValue) {
        return userRepository.findUsersBySearchValue(serachValue);
    }

    public List<OrganizationEntity> getUsersOrganizations() {
        UserEntity user = getCurrentUser();
        return userRepository.findOrganizationByUsers(user.getId());
    }

    public List<JoinRequestEntity> getPendingUsersJoinRequests() {
        UserEntity user = getCurrentUser();
        return joinRequestRepository.findByUserIdAndStatus(user.getId(), JoinRequestStatus.PENDING);
    }

    public List<UserEntity> getUsersOfOrganization(UUID orgId) {
        return userRepository.findUsersByOrganization(orgId);
    }

    private OrganizationEntity getOrganization(UUID orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
    }

    UserEntity getUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public JoinRequestEntity sendJoinRequest(UUID orgId, UUID userId) {
        OrganizationEntity org = getOrganization(orgId);
        UserEntity user = getUser(userId);

        // Preverimo ali je že član organizacije
        if(!membershipRepository.findByUserIdAndOrganizationId(userId, orgId).isEmpty()) {
            throw new RuntimeException("User is already a member of organization");
        }

        // Preverimo ali je uporabnik že poslal prošnjo
        List<JoinRequestEntity> requests = joinRequestRepository.findByUserIdAndOrganizationId(userId, orgId)
                .stream()
                .filter(jr -> jr.getStatus() == JoinRequestStatus.PENDING).toList();
        if (!requests.isEmpty()) {
            throw new RuntimeException("User already has a pending join request");
        }

        // Preverimo ali je uporabnik dobil povabilo
        List<InvitationEntity> pendingInvites =
                invitationRepository.findByOrganizationIdAndStatusAndUserId(orgId, InvitationStatus.PENDING, userId);

        if (!pendingInvites.isEmpty()) {
            throw new RuntimeException("User already has a pending invitation to this organization");
        }

        JoinRequestEntity request = new JoinRequestEntity();
        request.setUser(user);
        request.setOrganization(org);
        request.setStatus(JoinRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        request.setHandledAt(null);
        request.setHandledByUserId(null);

        JoinRequestEntity saved = joinRequestRepository.save(request);

        List<String> adminIds = membershipRepository.findByOrganizationIdAndRole(orgId, KeycloakRole.ORG_ADMIN).stream().map(OrganizationMembershipEntity::getUser).map(UserEntity::getKeycloakId).map(String::valueOf).toList();

        // Kafka event ob pošiljanju zahteve za vstop v organizacijo
        var event = new JoinRequestsSentEvent(
                request.getId(),
                adminIds,
                orgId,
                org.getName(),
                user.getKeycloakId(),
                user.getUsername(),
                Instant.now()
        );
        kafkaProducer.publishJoinRequestSentEvent(event);

        log.info("User {} requested to join organization {}", userId, orgId);
        return saved;
    }

    public UserEntity createLocalUserProfile(UUID keycloakId, String email, String username, String fisrtName, String lastName, Boolean emailConsent, Boolean smsConsent) {
        UserEntity user = new UserEntity();
        user.setKeycloakId(keycloakId);
        user.setEmail(email);
        user.setUsername(username);
        user.setFirstName(fisrtName);
        user.setLastName(lastName);
        user.setCreatedAt(LocalDateTime.now());
        user.setEmailConsent(emailConsent);
        user.setSmsConsent(smsConsent);
        return userRepository.save(user);
    }

    @Transactional
    public void provisionUser(UUID keycloakId, String email, String username, String fisrtName, String lastName) {

        Optional<UserEntity> existing = userRepository.findByKeycloakId(keycloakId);

        if (existing.isPresent()) {
            return;
        }

        UserEntity user = new UserEntity();
        user.setKeycloakId(keycloakId);
        user.setEmail(email);
        user.setUsername(username);
        user.setFirstName(fisrtName);
        user.setLastName(lastName);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        log.info("Provisioned new user: {} ({})", username, keycloakId);
    }
}
