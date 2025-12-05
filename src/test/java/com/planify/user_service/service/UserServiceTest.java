package com.planify.user_service.service;

import com.planify.user_service.event.KafkaProducer;
import com.planify.user_service.model.*;
import com.planify.user_service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private OrganizationMembershipRepository membershipRepository;

    @Mock
    private JoinRequestRepository joinRequestRepository;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private UserService userService;

    private UserEntity testUser;
    private OrganizationEntity testOrganization;
    private UUID testUserId;
    private UUID testOrgId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testOrgId = UUID.randomUUID();

        testUser = new UserEntity();
        testUser.setId(testUserId);
        testUser.setKeycloakId("keycloak-123");
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setCreatedAt(LocalDateTime.now());

        testOrganization = new OrganizationEntity();
        testOrganization.setId(testOrgId);
        testOrganization.setName("Test Organization");
        testOrganization.setSlug("test-org");
        testOrganization.setType(OrganizationType.BUSINESS);
        testOrganization.setCreatedByUserId(testUserId);
    }

    @Test
    void testSyncUserFromToken_ExistingUser() {
        // Arrange
        mockSecurityContext();
        when(userRepository.findByKeycloakId("keycloak-123"))
                .thenReturn(Optional.of(testUser));

        // Act
        UserEntity result = userService.syncUserFromToken();

        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getId());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).findByKeycloakId("keycloak-123");
        verify(userRepository, never()).save(any());
    }

    @Test
    void testSyncUserFromToken_NewUser() {
        // Arrange
        mockSecurityContext();
        when(userRepository.findByKeycloakId("keycloak-123"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> {
                    UserEntity user = invocation.getArgument(0);
                    user.setId(testUserId);
                    return user;
                });

        // Act
        UserEntity result = userService.syncUserFromToken();

        // Assert
        assertNotNull(result);
        assertEquals("keycloak-123", result.getKeycloakId());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void testGetCurrentUser() {
        // Arrange
        mockSecurityContext();
        when(userRepository.findByKeycloakId("keycloak-123"))
                .thenReturn(Optional.of(testUser));

        // Act
        UserEntity result = userService.getCurrentUser();

        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getId());
    }

    @Test
    void testDeleteUser() {
        // Arrange
        when(userRepository.findById(testUserId))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(UserEntity.class)))
                .thenReturn(testUser);

        // Act
        userService.deleteUser(testUserId);

        // Assert
        verify(userRepository).findById(testUserId);
        verify(userRepository).save(any(UserEntity.class));
        // verify(auditService).logAction(eq(testUserId), eq(testUserId), eq("DELETE"),
        // eq("USER"), eq(testUserId), anyString());
    }

    @Test
    void testDeleteUser_NotFound() {
        // Arrange
        when(userRepository.findById(testUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.deleteUser(testUserId));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testExportUserData() {
        // Arrange
        testUser.setMemberships(new HashSet<>());

        when(userRepository.findByIdAndDeletedAtIsNull(testUserId))
                .thenReturn(Optional.of(testUser));

        // Act
        Map<String, Object> result = userService.exportUserData(testUserId);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("user"));
        assertTrue(result.containsKey("memberships"));
        assertEquals(testUser, result.get("user"));
        assertEquals(testUser.getMemberships(), result.get("memberships"));
    }

    @Test
    void testExportUserData_DeletedUser() {
        // Arrange
        when(userRepository.findByIdAndDeletedAtIsNull(testUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.exportUserData(testUserId));
    }

    @Test
    void testGetUsers() {
        // Arrange
        List<UserEntity> users = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<UserEntity> result = userService.getUsers();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUserId, result.get(0).getId());
    }

    @Test
    void testGetUsersOfOrganization() {
        // Arrange
        List<UserEntity> users = Arrays.asList(testUser);
        when(userRepository.findUsersByOrganization(testOrgId)).thenReturn(users);

        // Act
        List<UserEntity> result = userService.getUsersOfOrganization(testOrgId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUserId, result.get(0).getId());
    }

    @Test
    void testSendJoinRequest_Success() {
        // Arrange
        when(organizationRepository.findById(testOrgId))
                .thenReturn(Optional.of(testOrganization));
        when(userRepository.findByIdAndDeletedAtIsNull(testUserId))
                .thenReturn(Optional.of(testUser));
        when(membershipRepository.findByUserIdAndOrganizationId(testUserId, testOrgId))
                .thenReturn(Optional.empty());
        when(joinRequestRepository.findByUserIdAndOrganizationId(testUserId, testOrgId))
                .thenReturn(Optional.empty());
        when(invitationRepository.findByOrganizationIdAndStatusAndUserId(
                testOrgId, InvitationStatus.PENDING, testUserId))
                .thenReturn(Collections.emptyList());

        JoinRequestEntity savedRequest = new JoinRequestEntity();
        savedRequest.setId(UUID.randomUUID());
        savedRequest.setUser(testUser);
        savedRequest.setOrganization(testOrganization);
        savedRequest.setStatus(JoinRequestStatus.PENDING);

        when(joinRequestRepository.save(any(JoinRequestEntity.class)))
                .thenReturn(savedRequest);

        // Act
        JoinRequestEntity result = userService.sendJoinRequest(testOrgId, testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(JoinRequestStatus.PENDING, result.getStatus());
        verify(joinRequestRepository).save(any(JoinRequestEntity.class));
        verify(kafkaProducer).publishJoinRequestEvent(any());
    }

    @Test
    void testSendJoinRequest_AlreadyMember() {
        // Arrange
        OrganizationMembershipEntity membership = new OrganizationMembershipEntity();
        when(organizationRepository.findById(testOrgId))
                .thenReturn(Optional.of(testOrganization));
        when(userRepository.findByIdAndDeletedAtIsNull(testUserId))
                .thenReturn(Optional.of(testUser));
        when(membershipRepository.findByUserIdAndOrganizationId(testUserId, testOrgId))
                .thenReturn(Optional.of(membership));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> userService.sendJoinRequest(testOrgId, testUserId));
        verify(joinRequestRepository, never()).save(any());
    }

    @Test
    void testSendJoinRequest_PendingRequestExists() {
        // Arrange
        JoinRequestEntity existingRequest = new JoinRequestEntity();
        existingRequest.setStatus(JoinRequestStatus.PENDING);

        when(organizationRepository.findById(testOrgId))
                .thenReturn(Optional.of(testOrganization));
        when(userRepository.findByIdAndDeletedAtIsNull(testUserId))
                .thenReturn(Optional.of(testUser));
        when(membershipRepository.findByUserIdAndOrganizationId(testUserId, testOrgId))
                .thenReturn(Optional.empty());
        when(joinRequestRepository.findByUserIdAndOrganizationId(testUserId, testOrgId))
                .thenReturn(Optional.of(existingRequest));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> userService.sendJoinRequest(testOrgId, testUserId));
        verify(joinRequestRepository, never()).save(any());
    }

    @Test
    void testCreateLocalUserProfile() {
        // Arrange
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> {
                    UserEntity user = invocation.getArgument(0);
                    user.setId(testUserId);
                    return user;
                });

        // Act
        UserEntity result = userService.createLocalUserProfile(
                "keycloak-456", "new@example.com", "newuser", "New", "User");

        // Assert
        assertNotNull(result);
        assertEquals("keycloak-456", result.getKeycloakId());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("newuser", result.getUsername());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void testProvisionUser_NewUser() {
        // Arrange
        when(userRepository.findByKeycloakId("keycloak-789"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class)))
                .thenReturn(testUser);

        // Act
        userService.provisionUser("keycloak-789", "provision@example.com",
                "provisionuser", "Provision", "User");

        // Assert
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void testProvisionUser_ExistingUser() {
        // Arrange
        when(userRepository.findByKeycloakId("keycloak-123"))
                .thenReturn(Optional.of(testUser));

        // Act
        userService.provisionUser("keycloak-123", "test@example.com",
                "testuser", "Test", "User");

        // Assert
        verify(userRepository, never()).save(any());
    }

    private void mockSecurityContext() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", "test@example.com");
        claims.put("given_name", "Test");
        claims.put("family_name", "User");

        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                claims);
        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "keycloak-123")
                .claim("email", "test@example.com")
                .claim("given_name", "Test")
                .claim("family_name", "User")
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
