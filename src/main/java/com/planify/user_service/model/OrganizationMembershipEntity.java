package com.planify.user_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organization_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "organization_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMembershipEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationRole role = OrganizationRole.GUEST;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
