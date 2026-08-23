package com.gitpulse.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a GitHub Actions CI/CD workflow execution run.
 *
 * Why JPA Entity:
 * Maintains a live feed of CI/CD builds, test executions, and deployments.
 * Enables live stream updates, rerun triggering, and pipeline health metrics.
 */
@Entity
@Table(name = "workflow_runs", indexes = {
        @Index(name = "idx_run_github_id", columnList = "github_run_id"),
        @Index(name = "idx_run_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_run_id", unique = true)
    private Long githubRunId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id")
    private RepositorySummary repository;

    @Column(name = "workflow_name", nullable = false)
    private String workflowName;

    @Column(name = "event")
    private String event;

    @Column(name = "status", nullable = false)
    private String status; // queued, in_progress, completed

    @Column(name = "conclusion")
    private String conclusion; // success, failure, cancelled, neutral, timed_out, skipped

    @Column(name = "commit_message", columnDefinition = "TEXT")
    private String commitMessage;

    @Column(name = "commit_sha", length = 64)
    private String commitSha;

    @Column(name = "branch")
    private String branch;

    @Column(name = "author")
    private String author;

    @Column(name = "duration_seconds")
    @Builder.Default
    private Long durationSeconds = 0L;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
