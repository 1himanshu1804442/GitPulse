package com.gitpulse.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a synchronized GitHub Repository.
 *
 * Why cached in database:
 * Caching repository metadata in PostgreSQL provides sub-millisecond query responses
 * for the React frontend dashboard while drastically reducing GitHub API rate-limit consumption.
 */
@Entity
@Table(name = "repository_summaries", indexes = {
        @Index(name = "idx_repo_fullname", columnList = "full_name", unique = true),
        @Index(name = "idx_repo_owner", columnList = "owner")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositorySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_repo_id", unique = true)
    private Long githubRepoId;

    @Column(nullable = false)
    private String name;

    @Column(name = "full_name", nullable = false, unique = true)
    private String fullName;

    @Column(nullable = false)
    private String owner;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "stars_count")
    @Builder.Default
    private Integer starsCount = 0;

    @Column(name = "forks_count")
    @Builder.Default
    private Integer forksCount = 0;

    @Column(name = "open_issues_count")
    @Builder.Default
    private Integer openIssuesCount = 0;

    @Column(name = "watchers_count")
    @Builder.Default
    private Integer watchersCount = 0;

    @Column(name = "default_branch")
    @Builder.Default
    private String defaultBranch = "main";

    @Column(name = "language")
    private String language;

    @Column(name = "html_url")
    private String htmlUrl;

    @Column(name = "is_private")
    @Builder.Default
    private Boolean isPrivate = false;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        if (starsCount == null) starsCount = 0;
        if (forksCount == null) forksCount = 0;
        if (openIssuesCount == null) openIssuesCount = 0;
        if (watchersCount == null) watchersCount = 0;
        if (isPrivate == null) isPrivate = false;
        if (lastSyncedAt == null) lastSyncedAt = LocalDateTime.now();
    }
}
