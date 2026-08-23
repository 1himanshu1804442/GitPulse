package com.gitpulse.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Pull Requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequestDTO {
    private Long id;
    private Long githubPrId;
    private String repoFullName;
    private Integer prNumber;
    private String title;
    private String author;
    private String state;
    private Boolean isDraft;
    private Boolean draft;
    private Integer additions;
    private Integer deletions;
    private Integer commentsCount;
    private String htmlUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
