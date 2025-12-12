package com.planify.user_service.controller;

import com.planify.user_service.model.*;
import com.planify.user_service.service.OrganizationService;
import com.planify.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserService userService;

    /**
     * Ustvari novo organizacijo v sistemu
     * @param body: podatki o organizaciji, ki jo hočemo ustvariti
     * @return objekt ustvarjene organizacije
     */
    @Operation(
            summary = "Ustvari novo organizacijo",
            description = "Ustvari novo organizacijo in uporabnika, ki bo administrator te organizacije."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Organizacija ustvarjena"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri ustvarjanju organizacije")
    })
    @PostMapping
    public ResponseEntity<OrganizationEntity> createOrganization(@RequestBody Organization body) {
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
            summary = "Povabi uporabnika v organizacijo",
            description = "Povabi izbranega uporabnika, da postane del organizacije. To lahko naredi le administrator organizacije."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Povabilo poslano"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri pošiljanju povabila"),
            @ApiResponse(responseCode = "403", description = "Prijavljeni uporabnik ni administrator organizacije")
    })
    @PostMapping("/{orgId}/invite")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> inviteUser(
            @PathVariable UUID orgId,
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
     * Odstranimo uporabnika iz organizacije
     * @param orgId: Id organizacije, iz katere hočemo odstraniti uporabnika
     * @param userId: Id uporabnika, katerega hočemo odstraniti
     * @return sporočilo, da je bil uspešno izbrisan
     */
    @Operation(
            summary = "Odstrani uporabnika iz organizacije",
            description = "Izbranega uporabnika odstrani iz organizacije. To lahko naredi le administrator organizacije."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Uporabnik uspe[no odstranjen"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri odstranjevanju uporabnika"),
            @ApiResponse(responseCode = "403", description = "Prijavljeni uporabnik ni administrator organizacije")
    })
    @DeleteMapping("/{orgId}/members/{userId}")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> removeUser(
            @PathVariable UUID orgId,
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
     * Odobri poslano zahtevo za vstop v organizacijo
     * @param orgId: Id organizacije, za katero hočemo odobriti zahtevo
     * @param requestId: Id zahteve, ki jo hočemo odobriti
     * @return vrnemo sporočilo, da je uspešno odobrena zahteva
     */
    @Operation(
            summary = "Sprejme prošnjo za vstop v organizacijo",
            description = "Sprejme prošnjo določenega uporabnika in mu doda vlogo GUEST v organizaciji. To lahko naredi le administrator organizacije."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prošnja uspešno sprejeta"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri sprejemanju prošnje"),
            @ApiResponse(responseCode = "403", description = "Prijavljeni uporabnik ni administrator organizacije")
    })
    @PostMapping("/{orgId}/join-request/{requestId}/approve")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> approveJoinRequest(
            @PathVariable UUID orgId,
            @PathVariable UUID requestId) {
        try{
            // pridobimo uporabnika, ki je poslal zahtevek
            UserEntity user = userService.getCurrentUser();

            organizationService.approveJoinRequest(orgId, user.getId(), requestId);
            return ResponseEntity.ok("Join request approved");
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
            summary = "Zavrne prošnjo za vstop v organizacijo",
            description = "Zavrne prošnjo določenega uporabnika. To lahko naredi le administrator organizacije."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prošnja uspešno zavrnjena"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri zavračanju prošnje"),
            @ApiResponse(responseCode = "403", description = "Prijavljeni uporabnik ni administrator organizacije")
    })
    @PostMapping("/{orgId}/join-request/{requestId}/reject")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> rejectJoinRequest(
            @PathVariable UUID orgId,
            @PathVariable UUID requestId) {
        try {
            // pridobimo uporabnika, ki je poslal zahtevek
            UserEntity user = userService.getCurrentUser();

            organizationService.rejectJoinRequest(orgId, requestId, user.getId());
            return ResponseEntity.ok("Join request rejected");
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
            summary = "Pridobi prošnje za organizacijo",
            description = "Pridobi seznam prošenj, ki so bile poslane za vstop v določeno organizacijo. To lahko vidi le administrator organizacije."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prošnja uspešno pridobljene"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri pridobivanju prošnje"),
            @ApiResponse(responseCode = "403", description = "Prijavljeni uporabnik ni administrator organizacije")
    })
    @GetMapping("/{orgId}/join-requests")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<List<JoinRequestEntity>> getJoinRequests(
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
     * @param newRole: nova vloga, ki jo hočemo dodeliti uporabniku
     * @return obvestilo, da je vloga bila uspešno zamenjana
     */
    @Operation(
            summary = "Spremeni vlogo uporabnika v organizaciji",
            description = "Spremeni vlogo določenega uporabnika znotraj organizacije. To lahko naredi le administrator organizacije."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vloga uspešno sprejeta"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri spreminjanju vloge"),
            @ApiResponse(responseCode = "403", description = "Prijavljeni uporabnik ni administrator organizacije")
    })
    @PostMapping("/{orgId}/members/{userId}/role")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> changeUserRole(
            @PathVariable UUID orgId,
            @PathVariable UUID userId,
            @RequestParam KeycloakRole newRole) {
        try{
            // pridobimo uporabnika, ki je poslal zahtevek
            UserEntity user = userService.getCurrentUser();

            organizationService.changeUserRole(orgId, userId, newRole, user.getId());
            return ResponseEntity.ok("Role updated");
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }


    /**
     * Pridobi vse člane organizacije
     * @param orgId: id organizacije, za katero hočemo pridobiti člane
     * @return seznam članstev v organizaciji (uporabniki in njihove vloge)
     */
    @Operation(
            summary = "Pridobi člane organizacije",
            description = "Pridobi seznam vseh članov znotraj organizacije (skupaj z njihovimi vlogami). To lahko vidi le administrator organizacije."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Članstva uspešno pridobljena"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri pridobivanju članstev"),
            @ApiResponse(responseCode = "403", description = "Prijavljeni uporabnik ni administrator organizacije")
    })
    @GetMapping("/{orgId}/members")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<List<OrganizationMembershipEntity>> getMembers(
            @PathVariable UUID orgId) {
        try{
            List<OrganizationMembershipEntity> memberships = organizationService.getOrganizationUsers(orgId);
            return ResponseEntity.ok(memberships);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }
}
