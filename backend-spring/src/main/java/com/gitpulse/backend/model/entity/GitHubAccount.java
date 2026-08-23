package com.gitpulse.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a connected GitHub user account or organizational profile.
 *
 * Why JPA Entity:
 * Maps to the relational table `github_accounts` in PostgreSQL (or H2 for tests),
 * persisting user auth state, profile image, and last synchronization timestamp.
 */
@Entity
@Table(name = "github_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "token_encrypted", length = 1024)
    private String tokenEncrypted;

    @Column(name = "is_token_valid")
    @Builder.Default
    private Boolean isTokenValid = true;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isTokenValid == null) {
            isTokenValid = true;
        }
    }
}
