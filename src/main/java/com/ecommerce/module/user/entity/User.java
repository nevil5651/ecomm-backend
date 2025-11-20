package com.ecommerce.module.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * User entity matching tables.md users table.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(length = 150, unique = true, nullable = false)
    private String email;

    @Column(columnDefinition = "text", nullable = false)
    private String password;

    @Column(length = 20, nullable = false)
    private String role; // CUSTOMER | VENDOR | ADMIN | STAFF

    @Column(name = "is_email_verified")
    private Boolean isEmailVerified = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}