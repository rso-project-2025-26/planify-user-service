package com.planify.user_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "organizations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug; // Unikatni identifikator v URL-ju

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationType type = OrganizationType.PERSONAL;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @JsonIgnore
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrganizationMembershipEntity> memberships = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<JoinRequestEntity> joinRequests = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InvitationEntity> invitations = new HashSet<>();
}
