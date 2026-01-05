package com.planify.user_service.controller;

import com.planify.user_service.model.*;
import com.planify.user_service.service.OrganizationService;
import com.planify.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Organization management and membership endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserService userService;

    /**
     * Ustvari novo organizacijo v sistemu
     * @param body: podatki o organizaciji, ki jo hočemo ustvariti
     * @return objekt ustvarjene organizacije
     */
    @Operation(
            summary = "Create new organization",
            description = "Create new organization and assign user as administrator of this organization."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Organization created"),
            @ApiResponse(responseCode = "500", description = "Error occurred while creating organization")
    })
    @PostMapping
    public ResponseEntity<OrganizationEntity> createOrganization(
            @Parameter(required = true)
            @RequestBody Organization body) {
        try{
            OrganizationEntity org = organizationService.createOrganization(body);
            return ResponseEntity.status(201).body(org);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(null);
        }

    }


    /**
     * Admin povabi uporabnika v organizacijo
     * @param orgId: Id organizacije, v katero hočemo uporabnika povabiti
     * @param userId: Id uporabnika, ki ga hočemo povabiti v organizacijo
     * @param role: vlogo, katero hočemo uporabniku dodati
     * @return objekt ustvarjenega povabila
     */
    @Operation(
            summary = "Invite user to organization",
            description = "Invite selected user to become part of organization. Only organization administrator can do this."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invitation sent"),
            @ApiResponse(responseCode = "500", description = "Error occurred while sending invitation"),
            @ApiResponse(responseCode = "401", description = "Logged in user is not organization administrator")
    })
    @PostMapping("/{orgId}/invite")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> inviteUser(
            @Parameter(required = true)
            @PathVariable UUID orgId,
            @Parameter(required = true)
            @RequestParam UUID userId,
            @RequestParam(required = false) KeycloakRole role) {
        try {
            // pridobimo uporabnika, ki je poslal zahtevek
            UserEntity user = userService.getCurrentUser();

            InvitationEntity invitation = organizationService.inviteUserToOrganization(
                    orgId, userId, user.getId(), role);

            return ResponseEntity.status(201).body(invitation);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }

    }

    /**
     * Pridobi id organizacije glede na prijavljenega administratora organizacije.
     * @return Identifikator organizacije.
     */
    @Operation(
            summary = "Get organization identifier of currently logged in organization administrator.",
            description = "Returns organization identifier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Organization identifier successfully retrieved"),
            @ApiResponse(responseCode = "500", description = "Error occurred while retrieving organization identifier"),
            @ApiResponse(responseCode = "401", description = "Logged in user is not organization administrator")
    })
    @GetMapping("/admin/org")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<?> getOrganizationsId() {
        try{
            // pridobimo uporabnika, ki je poslal zahtevek
            UserEntity admin = userService.getCurrentUser();
            OrganizationSummary org = organizationService.getOrganizationByAdmin(admin.getId());
            return ResponseEntity.ok(List.of(org));
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }


    /**
     * Odstranimo uporabnika iz organizacije
     * @param orgId: Id organizacije, iz katere hočemo odstraniti uporabnika
     * @param userId: Id uporabnika, katerega hočemo odstraniti
     * @return sporočilo, da je bil uspešno izbrisan
     */
    @Operation(
            summary = "Remove user from organization",
            description = "Remove selected user from organization. Only organization administrator can do this."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User successfully removed"),
            @ApiResponse(responseCode = "500", description = "Error occurred while removing user"),
            @ApiResponse(responseCode = "401", description = "Logged in user is not organization administrator")
    })
    @DeleteMapping("/{orgId}/members/{userId}")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> removeUser(
            @Parameter(required = true)
            @PathVariable UUID orgId,
            @Parameter(required = true)
            @PathVariable UUID userId) {
        try{
            // pridobimo uporabnika, ki je poslal zahtevek
            UserEntity user = userService.getCurrentUser();

            organizationService.removeUserFromOrganization(orgId, userId, user.getId());
            return ResponseEntity.status(204).body("User removed successfully");
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }

    }

    /**
     * Odstranimo uporabnika iz organizacije
     * @param orgId: Id organizacije, iz katere hočemo odstraniti uporabnika
     * @return sporočilo, da je bil uspešno izbrisan
     */
    @Operation(
            summary = "Remove currently logged in user from organization",
            description = "Remove currently logged in user from organization. Only organization administrator can do this."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User successfully removed"),
            @ApiResponse(responseCode = "500", description = "Error occurred while removing user"),
            @ApiResponse(responseCode = "401", description = "Logged in user is not organization administrator")
    })
    @DeleteMapping("/me/memberships/{orgId}")
    @PreAuthorize("hasRole('UPORABNIK')")
    public ResponseEntity<?> removeCurrentUser(
            @Parameter(required = true)
            @PathVariable UUID orgId) {
        try{
            // pridobimo uporabnika, ki je poslal zahtevek
            UserEntity user = userService.getCurrentUser();

            organizationService.removeMyselfFromOrganization(orgId, user.getId());
            return ResponseEntity.status(204).body("User removed successfully");
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }

    }

    /**
     * Odobri poslano zahtevo za vstop v organizacijo
     * @param orgId: Id organizacije, za katero hočemo odobriti zahtevo
     * @param requestId: Id zahteve, ki jo hočemo odobriti
     * @return vrnemo sporočilo, da je uspešno odobrena zahteva
     */
    @Operation(
            summary = "Accept request to join organization",
            description = "Accept request from specific user and assign them GUEST role in organization. Only organization administrator can do this."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request successfully accepted"),
            @ApiResponse(responseCode = "500", description = "Error occurred while accepting request"),
            @ApiResponse(responseCode = "401", description = "Logged in user is not organization administrator")
    })
    @PostMapping("/{orgId}/join-request/{requestId}/approve")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> approveJoinRequest(
            @Parameter(required = true)
            @PathVariable UUID orgId,
            @Parameter(required = true)
            @PathVariable UUID requestId) {
        try{
            // pridobimo uporabnika, ki je poslal zahtevek
            UserEntity user = userService.getCurrentUser();

            organizationService.approveJoinRequest(orgId, user.getId(), requestId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }

    }


    /**
     * Zavrnemo zahtevo za vstop v organizacijo
     * @param orgId: Id organizacije za katero zavrnemo dostop
     * @param requestId: Id zahteve, ki jo hočemo zavrniti
     * @return odgovor, da je zahteva uspešno zavrnejna
     */
    @Operation(
            summary = "Reject request to join organization",
            description = "Reject request from specific user. Only organization administrator can do this."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request successfully rejected"),
            @ApiResponse(responseCode = "500", description = "Error occurred while rejecting request"),
            @ApiResponse(responseCode = "401", description = "Logged in user is not organization administrator")
    })
    @PostMapping("/{orgId}/join-request/{requestId}/reject")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> rejectJoinRequest(
            @Parameter(required = true)
            @PathVariable UUID orgId,
            @Parameter(required = true)
            @PathVariable UUID requestId) {
        try {
            // pridobimo uporabnika, ki je poslal zahtevek
            UserEntity user = userService.getCurrentUser();

            organizationService.rejectJoinRequest(orgId, requestId, user.getId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }


    /**
     * Pridobi vse neodgovorjene zahteve za pridružitev organizaciji
     * @param orgId: id organizacije, za katero hočemo pridobiti zahteve
     * @return seznam neodgovorjenih (PENDING) zahtev
     */
    @Operation(
            summary = "Get requests for organization",
            description = "Get list of requests sent to join specific organization. Only organization administrator can see this."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Requests successfully retrieved"),
            @ApiResponse(responseCode = "500", description = "Error occurred while retrieving requests"),
            @ApiResponse(responseCode = "401", description = "Logged in user is not organization administrator")
    })
    @GetMapping("/{orgId}/join-requests")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<List<JoinRequestEntity>> getJoinRequests(
            @Parameter(required = true)
            @PathVariable UUID orgId) {
        try{
            // pridobimo uporabnika, ki je poslal zahtevek
            List<JoinRequestEntity> requests = organizationService.getJoinRequests(orgId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }


    /**
     * Spremeni vlogo v organizaciji uporbaniku
     * @param orgId: id organizacije
     * @param userId: Id uporabnika, kateremu želimo spremeniti vlogo v organizaciji
     * @param newRoles: seznam novih vlog, ki jih hočemo dodeliti uporabniku
     * @return obvestilo, da je vloga bila uspešno zamenjana
     */
    @Operation(
            summary = "Change user role in organization",
            description = "Change role of specific user within organization. Only organization administrator can do this."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role successfully accepted"),
            @ApiResponse(responseCode = "500", description = "Error occurred while changing role"),
            @ApiResponse(responseCode = "401", description = "Logged in user is not organization administrator")
    })
    @PutMapping("/{orgId}/members/{userId}/role")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> changeUserRole(
            @Parameter(required = true)
            @PathVariable UUID orgId,
            @Parameter(required = true)
            @PathVariable UUID userId,
            @Parameter(required = true)
            @RequestParam List<KeycloakRole> newRoles) {
        try{
            // pridobimo uporabnika, ki je poslal zahtevek
            UserEntity user = userService.getCurrentUser();
            organizationService.changeUserRoles(orgId, userId, newRoles, user.getId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }



    /**
     * Pridobimo vse uporabnike organizacije
     * @param orgId: Id organizacije, za katero hočemo pridobiti uporabnike in njihove vloge
     * @return seznam uporabnikov
     */
    @Operation(
            summary = "Get all users of organization",
            description = "Returns list of all members of specific organization. Only administrator can see the list of users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List successfully retrieved"),
            @ApiResponse(responseCode = "500", description = "Error occurred while retrieving user list"),
            @ApiResponse(responseCode = "401", description = "Logged in user is not organization administrator")
    })
    @GetMapping("/{orgId}/members")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> getOrganizationsUsers(
            @Parameter(required = true)
            @PathVariable UUID orgId) {
        try{
            // pridobimo uporabnika, ki je poslal zahtevek
            List<UserRoles> users = organizationService.getUsersAndRoles(orgId);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    /**
     * Pridobimo vse uporabnike organizacije
     * @param orgId: Id organizacije, za katero hočemo pridobiti uporabnike in njihove vloge
     * @return seznam keycloak identifikatorjev uporabnikov
     */
    @Operation(
            summary = "Get all users of organization",
            description = "Returns list of Keycloak identifiers of all members of specific organization. Only administrator can see the list of users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List successfully retrieved"),
            @ApiResponse(responseCode = "500", description = "Error occurred while retrieving user list"),
            @ApiResponse(responseCode = "401", description = "Logged in user is not organization administrator")
    })
    @GetMapping("/{orgId}/keycloak/members")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> getOrganizationsKeycloakUsers(
            @Parameter(required = true)
            @PathVariable UUID orgId) {
        try{
            List<UUID> users = organizationService.getKeycloakUsers(orgId);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    /**
     * Pridobimo vse organizacije v naši bazi glede na iskalno vrednost
     * @return seznam organizacij
     */
    @Operation(
            summary = "Get organizations registered in system",
            description = "Get list of all organizations in application whose slug starts with search value."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List successfully retrieved"),
            @ApiResponse(responseCode = "500", description = "Error occurred while retrieving list"),
            @ApiResponse(responseCode = "401", description = "Logged in user is not organization administrator")
    })
    @PreAuthorize("hasRole('UPORABNIK')")
    @GetMapping("/search")
    public ResponseEntity<List<OrganizationEntity>> searchOrgs(
            @Parameter(required = true)
            @RequestParam String query) {
        try{
            List<OrganizationEntity> orgs = organizationService.searchOrgs(query);
            return ResponseEntity.ok(orgs);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

}
