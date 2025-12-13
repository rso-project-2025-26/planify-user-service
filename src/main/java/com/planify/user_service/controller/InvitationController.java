package com.planify.user_service.controller;

import com.planify.user_service.model.InvitationEntity;
import com.planify.user_service.model.InvitationStatus;
import com.planify.user_service.model.KeycloakRole;
import com.planify.user_service.model.UserEntity;
import com.planify.user_service.service.InvitationsService;
import com.planify.user_service.service.OrganizationService;
import com.planify.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
public class InvitationController {

    private final InvitationsService invitationsService;
    private final UserService userService;
    private final OrganizationService organizationService;

    /**
     * Pridobi vsa povabila v sistemu
     * @return seznam vseh povabil v sistemu
     */
    @Operation(
            summary = "Pridobi vsa povabila",
            description = "Vrne seznam vseh povabil v celotnem sistemu. Dostop ima le administrator."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Povabila uspešno pridobljena"),
            @ApiResponse(responseCode = "500", description = "Pri pridobivanju je prišlo do napake"),
            @ApiResponse(responseCode = "403", description = "Uporabnik ni administrator aplikacije.")
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
            summary = "Pridobi vsa neodgovorjena povabila neke organizacije",
            description = "Vrne seznam vseh neodgovorjenih povabil za neko organizacijo. Dostop ima le administrator organizacije."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Neodgovorjena povabila uspešno pridobljena"),
            @ApiResponse(responseCode = "500", description = "Pri pridobivanju je prišlo do napake"),
            @ApiResponse(responseCode = "403", description = "Uporabnik ni administrator organizacije.")
    })
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @GetMapping("/{orgId}/pending")
    public ResponseEntity<?> getInvitations(@PathVariable UUID orgId) {
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
            summary = "Pridobi povabila za trenutnega uporabnika",
            description = "Vrne seznam povabil, ki so naslovljena na trenutno prijavljenega uporabnika."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Povabila uspešno pridobljena"),
            @ApiResponse(responseCode = "500", description = "Pri pridobivanju je prišlo do napake"),
            @ApiResponse(responseCode = "403", description = "Uporabnik ni prijavljen v aplikaciji")
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
            summary = "Sprejmi povabilo",
            description = "Trenutno prijavljen uporabnik sprejme povabilo v organizacijo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Povabilo sprejeto"),
            @ApiResponse(responseCode = "500", description = "Pri sprejemanju povabil je prišlo do napake"),
            @ApiResponse(responseCode = "403", description = "Uporabnik ni prijavljen v aplikaciji")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{invitationToken}/accept")
    public ResponseEntity<InvitationEntity> acceptInvitation(
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
            summary = "Zavrni povabilo",
            description = "Trenutno prijavljen uporabnik zavrne povabilo v organizacijo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Povabilo zavrnjeno"),
            @ApiResponse(responseCode = "500", description = "Pri zavračanju povabila je prišlo do napke"),
            @ApiResponse(responseCode = "402", description = "Uporabnik ni prijavljen v aplikaciji")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{invitationToken}/decline")
    public ResponseEntity<Void> declineInvitation(
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
