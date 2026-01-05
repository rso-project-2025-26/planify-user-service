package com.planify.user_service.controller;

import com.planify.user_service.model.InvitationEntity;
import com.planify.user_service.model.InvitationStatus;
import com.planify.user_service.model.KeycloakRole;
import com.planify.user_service.model.UserEntity;
import com.planify.user_service.service.InvitationsService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole;


@Slf4j
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
@Tag(name = "Invitations", description = "Organization invitation management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class InvitationController {

    private final InvitationsService invitationsService;
    private final UserService userService;
    private final OrganizationService organizationService;

    /**
     * Pridobi vsa povabila v sistemu
     * @return seznam vseh povabil v sistemu
     */
    @Operation(
            summary = "Get all invitations",
            description = "Returns list of all invitations in the entire system. Only accessible by administrator."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitations successfully retrieved"),
            @ApiResponse(responseCode = "500", description = "Error occurred during retrieval"),
            @ApiResponse(responseCode = "401", description = "User is not application administrator.")
    })
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping
    public ResponseEntity<List<InvitationEntity>> getInvitations() {
        try{
            List<InvitationEntity> invitations = invitationsService.getInvitations();
            return ResponseEntity.ok(invitations);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Pridobi vsa neodgovorjena povabila za določeno organizacijo
     * @return seznam vseh neodgovorjenih povabil organizacije
     */
    @Operation(
            summary = "Get all unanswered invitations for an organization",
            description = "Returns list of all unanswered invitations for an organization. Only accessible by organization administrator."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unanswered invitations successfully retrieved"),
            @ApiResponse(responseCode = "500", description = "Error occurred during retrieval"),
            @ApiResponse(responseCode = "401", description = "User is not organization administrator.")
    })
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @GetMapping("/{orgId}/pending")
    public ResponseEntity<?> getInvitations(
            @Parameter(required = true)
            @PathVariable UUID orgId) {
        try{
            UserEntity user = userService.getCurrentUser();
            if (!organizationService.isUserOrgAdmin(orgId, user.getId())) {
                log.error("User {} wanted to view pending invitations of organization {} but is not admin of it.", user.getId(), orgId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User is not admin of the organization");
            }
            List<InvitationEntity> invitations = invitationsService.getInvitationsByOrganizationIdAndStatus(orgId, InvitationStatus.PENDING);
            return ResponseEntity.ok(invitations);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }


    /**
     * Pridobi seznam vseh povabil, za trenutno prijavljenega uporabnika
     * @return seznam povabil v organizacije za prijavljenega uporabnika
     */
    @Operation(
            summary = "Get invitations for current user",
            description = "Returns list of invitations addressed to currently logged in user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitations successfully retrieved"),
            @ApiResponse(responseCode = "500", description = "Error occurred during retrieval"),
            @ApiResponse(responseCode = "401", description = "User is not logged into application")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/currentUser")
    public ResponseEntity<List<InvitationEntity>> getInvitationsByCurrentUser() {
        try{
            List<InvitationEntity> invitations = invitationsService.getInvitationsByCurrentUserIdAndStatus(InvitationStatus.PENDING);
            return ResponseEntity.ok(invitations);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }

    }

    /**
     * Trenutno prijavljen uporabnik sprejme povabila v organizacijo
     * @param invitationToken: token povabila, ki ga hoče sprejeti
     * @return objekt sprejetega povabila
     */
    @Operation(
            summary = "Accept invitation",
            description = "Currently logged in user accepts invitation to organization."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitation accepted"),
            @ApiResponse(responseCode = "500", description = "Error occurred while accepting invitation"),
            @ApiResponse(responseCode = "401", description = "User is not logged into application")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{invitationToken}/accept")
    public ResponseEntity<InvitationEntity> acceptInvitation(
            @Parameter(required = true)
            @PathVariable String invitationToken) {
        try {
            InvitationEntity invitation = invitationsService.acceptInvitation(invitationToken);
            return ResponseEntity.ok(invitation);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }

    }

    /**
     * Trenutno prijavljen uporabnik zavrne povabila v organizacijo
     * @param invitationToken: token povabila, ki ga hoče zavrniti
     * @return respons no content
     */
    @Operation(
            summary = "Decline invitation",
            description = "Currently logged in user declines invitation to organization."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Invitation declined"),
            @ApiResponse(responseCode = "500", description = "Error occurred while declining invitation"),
            @ApiResponse(responseCode = "401", description = "User is not logged into application")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{invitationToken}/decline")
    public ResponseEntity<Void> declineInvitation(
            @Parameter(required = true)
            @PathVariable String invitationToken) {
        try {
            invitationsService.declineInvitation(invitationToken);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.internalServerError().build();
        }

    }
}
