package com.planify.user_service.controller;

import com.planify.user_service.model.*;
import com.planify.user_service.service.OrganizationService;
import com.planify.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserService userService;

    /**
     * Ustvari novo organizacijo v sistemu
     * @param body: podatki o organizaciji, ki jo hočemo ustvariti
     * @return objekt ustvarjene organizacije
     */
    @PostMapping
    public ResponseEntity<OrganizationEntity> createOrganization(@RequestBody Organization body) {
        OrganizationEntity org = organizationService.createOrganization(body);
        return ResponseEntity.status(201).body(org);
    }


    /**
     * Admin povabi uporabnika v organizacijo
     * @param orgId: Id organizacije, v katero hočemo uporabnika povabiti
     * @param userId: Id uporabnika, ki ga hočemo povabiti v organizacijo
     * @param role: vlogo, katero hočemo uporabniku dodati
     * @return objekt ustvarjenega povabila
     */
    @PostMapping("/{orgId}/invite")
    @PreAuthorize("@orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> inviteUser(
            @PathVariable UUID orgId,
            @RequestParam UUID userId,
            @RequestParam(required = false) OrganizationRole role) {
        // pridobimo uporabnika, ki je poslal zahtevek
        UserEntity user = userService.getCurrentUser();

        InvitationEntity invitation = organizationService.inviteUserToOrganization(
                orgId, userId, user.getId(), role);

        return ResponseEntity.ok(invitation);
    }


    /**
     * Odstranimo uporabnika iz organizacije
     * @param orgId: Id organizacije, iz katere hočemo odstraniti uporabnika
     * @param userId: Id uporabnika, katerega hočemo odstraniti
     * @return sporočilo, da je bil uspešno izbrisan
     */
    @DeleteMapping("/{orgId}/members/{userId}")
    @PreAuthorize("@orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> removeUser(
            @PathVariable UUID orgId,
            @PathVariable UUID userId) {
        // pridobimo uporabnika, ki je poslal zahtevek
        UserEntity user = userService.getCurrentUser();

        organizationService.removeUserFromOrganization(orgId, userId, user.getId());
        return ResponseEntity.ok("User removed successfully");
    }


    /**
     * Odobri poslano zahtevo za vstop v organizacijo
     * @param orgId: Id organizacije, za katero hočemo odobriti zahtevo
     * @param requestId: Id zahteve, ki jo hočemo odobriti
     * @return vrnemo sporočilo, da je uspešno odobrena zahteva
     */
    @PostMapping("/{orgId}/join-request/{requestId}/approve")
    @PreAuthorize("@orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> approveJoinRequest(
            @PathVariable UUID orgId,
            @PathVariable UUID requestId) {
        // pridobimo uporabnika, ki je poslal zahtevek
        UserEntity user = userService.getCurrentUser();

        organizationService.approveJoinRequest(orgId, user.getId(), requestId);
        return ResponseEntity.ok("Join request approved");
    }


    /**
     * Zavrnemo zahtevo za vstop v organizacijo
     * @param orgId: Id organizacije za katero zavrnemo dostop
     * @param requestId: Id zahteve, ki jo hočemo zavrniti
     * @return odgovor, da je zahteva uspešno zavrnejna
     */
    @PostMapping("/{orgId}/join-request/{requestId}/reject")
    @PreAuthorize("@orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> rejectJoinRequest(
            @PathVariable UUID orgId,
            @PathVariable UUID requestId) {
        // pridobimo uporabnika, ki je poslal zahtevek
        UserEntity user = userService.getCurrentUser();

        organizationService.rejectJoinRequest(orgId, requestId, user.getId());
        return ResponseEntity.ok("Join request rejected");
    }


    /**
     * Pridobi vse neodgovorjene zahteve za pridružitev organizaciji
     * @param orgId: id organizacije, za katero hočemo pridobiti zahteve
     * @return seznam neodgovorjenih (PENDING) zahtev
     */
    @GetMapping("/{orgId}/join-requests")
    @PreAuthorize("@orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<List<JoinRequestEntity>> getJoinRequests(
            @PathVariable UUID orgId) {
        // pridobimo uporabnika, ki je poslal zahtevek
        List<JoinRequestEntity> requests = organizationService.getJoinRequests(orgId);
        return ResponseEntity.ok(requests);
    }


    /**
     * Spremeni vlogo v organizaciji uporbaniku
     * @param orgId: id organizacije
     * @param userId: Id uporabnika, kateremu želimo spremeniti vlogo v organizaciji
     * @param newRole: nova vloga, ki jo hočemo dodeliti uporabniku
     * @return obvestilo, da je vloga bila uspešno zamenjana
     */
    @PostMapping("/{orgId}/members/{userId}/role")
    @PreAuthorize("@orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> changeUserRole(
            @PathVariable UUID orgId,
            @PathVariable UUID userId,
            @RequestParam OrganizationRole newRole) {
        // pridobimo uporabnika, ki je poslal zahtevek
        UserEntity user = userService.getCurrentUser();

        organizationService.changeUserRole(orgId, userId, newRole, user.getId());
        return ResponseEntity.ok("Role updated");
    }


    /**
     * Pridobi vse člane organizacije
     * @param orgId: id organizacije, za katero hočemo pridobiti člane
     * @return seznam članstev v organizaciji (uporabniki in njihove vloge)
     */
    @GetMapping("/{orgId}/members")
    @PreAuthorize("@orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<List<OrganizationMembershipEntity>> getMembers(
            @PathVariable UUID orgId) {
        List<OrganizationMembershipEntity> memberships = organizationService.getOrganizationUsers(orgId);
        return ResponseEntity.ok(memberships);
    }
}
