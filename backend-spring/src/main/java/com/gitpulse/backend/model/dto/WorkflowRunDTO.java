package com.gitpulse.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for CI/CD Workflow Runs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRunDTO {
    private Long id;
    private Long githubRunId;
    private String repoFullName;
    private String workflowName;
    private String event;
    private String status;
    private String conclusion;
    private String commitMessage;
    private String commitSha;
    private String branch;
    private String author;
    private Long durationSeconds;
    private String htmlUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
