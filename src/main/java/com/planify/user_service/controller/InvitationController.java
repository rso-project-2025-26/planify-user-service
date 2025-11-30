package com.planify.user_service.controller;

import com.planify.user_service.model.InvitationEntity;
import com.planify.user_service.model.KeycloakRole;
import com.planify.user_service.service.InvitationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole;


@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    public final InvitationsService invitationsService;

    /**
     * Pridobi vsa povabila v sistemu
     * @return seznam vseh povabil v sistemu
     */
    @PreAuthorize("hasRole('administrator')")
    @GetMapping
    public ResponseEntity<List<InvitationEntity>> getInvitations() {
        List<InvitationEntity> invitations = invitationsService.getInvitations();
        return ResponseEntity.ok(invitations);
    }

    /**
     * Pridobi seznam vseh povabil, za trenutno prijavljenega uporabnika
     * @return seznam povabil v organizacije za prijavljenega uporabnika
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/currentUser")
    public ResponseEntity<List<InvitationEntity>> getInvitationsByCurrentUser() {
        List<InvitationEntity> invitations = invitationsService.getInvitationsByCurrentUserId();
        return ResponseEntity.ok(invitations);
    }

    /**
     * Trenutno prijavljen uporabnik sprejme povabila v organizacijo
     * @param invitationToken: token povabila, ki ga hoče sprejeti
     * @return objekt sprejetega povabila
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{invitationToken}/accept")
    public ResponseEntity<InvitationEntity> acceptInvitation(
            @PathVariable String invitationToken) {
        InvitationEntity invitation = invitationsService.acceptInvitation(invitationToken);
        return ResponseEntity.ok(invitation);
    }

    /**
     * Trenutno prijavljen uporabnik zavrne povabila v organizacijo
     * @param invitationToken: token povabila, ki ga hoče zavrniti
     * @return respons no content
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{invitationToken}/decline")
    public ResponseEntity<Void> declineInvitation(
            @PathVariable String invitationToken) {
        invitationsService.declineInvitation(invitationToken);
        return ResponseEntity.noContent().build();
    }
}
