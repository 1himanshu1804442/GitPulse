package com.gitpulse.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a GitHub Pull Request summary.
 *
 * Why JPA Entity:
 * Powers the developer review queue, showing PR age, draft status, lines changed (+/-),
 * and review urgency.
 */
@Entity
@Table(name = "pull_request_summaries", indexes = {
        @Index(name = "idx_pr_github_id", columnList = "github_pr_id"),
        @Index(name = "idx_pr_state", columnList = "state")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PullRequestSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_pr_id", unique = true)
    private Long githubPrId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id")
    private RepositorySummary repository;

    @Column(name = "pr_number", nullable = false)
    private Integer prNumber;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String state; // open, closed, merged

    @Column(name = "is_draft")
    @Builder.Default
    private Boolean isDraft = false;

    @Column(name = "draft")
    @Builder.Default
    private Boolean draft = false;

    @Column(name = "additions")
    @Builder.Default
    private Integer additions = 0;

    @Column(name = "deletions")
    @Builder.Default
    private Integer deletions = 0;

    @Column(name = "comments_count")
    @Builder.Default
    private Integer commentsCount = 0;

    @Column(name = "html_url")
    private String htmlUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (isDraft == null) isDraft = false;
        if (draft == null) draft = isDraft;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
