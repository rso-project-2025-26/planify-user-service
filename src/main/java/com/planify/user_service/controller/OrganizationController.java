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
     * Pridobi id organizacije glede na prijavljenega administratora organizacije.
     * @return Identifikator organizacije.
     */
    @Operation(
            summary = "Pridobi identifikator organizacije trenutno prijavljenega administratorja organizacije.",
            description = "Vrne identifikator organizacije."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uspešno pridobljen identifikator organizaije"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri pridobivanju identifikatorja organizacije"),
            @ApiResponse(responseCode = "403", description = "Prijavljeni uporabnik ni administrator organizacije")
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
     * Odstranimo uporabnika iz organizacije
     * @param orgId: Id organizacije, iz katere hočemo odstraniti uporabnika
     * @return sporočilo, da je bil uspešno izbrisan
     */
    @Operation(
            summary = "Odstrani trenutno prijavljenega uporabnika iz organizacije",
            description = "Trenutno prijavljenega uporabnika odstrani iz organizacije. To lahko naredi le administrator organizacije."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Uporabnik uspešno odstranjen"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri odstranjevanju uporabnika"),
            @ApiResponse(responseCode = "403", description = "Prijavljeni uporabnik ni administrator organizacije")
    })
    @DeleteMapping("/me/memberships/{orgId}")
    @PreAuthorize("hasRole('UPORABNIK')")
    public ResponseEntity<?> removeCurrentUser(
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
     * @param newRoles: seznam novih vlog, ki jih hočemo dodeliti uporabniku
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
    @PutMapping("/{orgId}/members/{userId}/role")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> changeUserRole(
            @PathVariable UUID orgId,
            @PathVariable UUID userId,
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
            summary = "Pridobimo vse uporabnike organizacije",
            description = "Vrne seznam vseh članov določene organizacije. Le administrator lahko vidi seznam uporabnikov."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uspešno pridobljen seznam"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri pridobivanju seznama uporabnikov"),
            @ApiResponse(responseCode = "403", description = "Prijavljeni uporabnik ni administrator organizacije")
    })
    @GetMapping("/{orgId}/members")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> getOrganizationsUsers(@PathVariable UUID orgId) {
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
            summary = "Pridobimo vse uporabnike organizacije",
            description = "Vrne seznam keycloak identifikatorjev vseh članov določene organizacije. Le administrator lahko vidi seznam uporabnikov."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uspešno pridobljen seznam"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri pridobivanju seznama uporabnikov"),
            @ApiResponse(responseCode = "403", description = "Prijavljeni uporabnik ni administrator organizacije")
    })
    @GetMapping("/{orgId}/keycloak/members")
    @PreAuthorize("hasRole('ORG_ADMIN') and @orgSecurity.isAdmin(#orgId, authentication)")
    public ResponseEntity<?> getOrganizationsKeycloakUsers(@PathVariable UUID orgId) {
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
            summary = "Pridobi orgnizacije prijavljene v sistem",
            description = "Pridobi seznam vseh organizacij v aplikaciji, katerih slug se začne z iskalno vrednostjo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seznam je uspešno pridobljen"),
            @ApiResponse(responseCode = "500", description = "Prišlo je do napake pri pridobivanju seznama"),
            @ApiResponse(responseCode = "403", description = "Prijavljeni uporabnik ni administrator organizacije")
    })
    @PreAuthorize("hasRole('UPORABNIK')")
    @GetMapping("/search")
    public ResponseEntity<List<OrganizationEntity>> searchOrgs(@RequestParam String query) {
        try{
            List<OrganizationEntity> orgs = organizationService.searchOrgs(query);
            return ResponseEntity.ok(orgs);
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

}
