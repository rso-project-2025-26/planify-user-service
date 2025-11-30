package com.planify.user_service.controller;

import com.planify.user_service.model.InvitationEntity;
import com.planify.user_service.model.KeycloakRole;
import com.planify.user_service.service.InvitationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole;


@Slf4j
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    public final InvitationsService invitationsService;

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
    @PostMapping("/currentUser")
    public ResponseEntity<List<InvitationEntity>> getInvitationsByCurrentUser() {
        try{
            List<InvitationEntity> invitations = invitationsService.getInvitationsByCurrentUserId();
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
