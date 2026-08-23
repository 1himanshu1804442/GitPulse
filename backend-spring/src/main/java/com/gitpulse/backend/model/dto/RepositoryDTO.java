package com.gitpulse.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for GitHub Repository data.
 *
 * Why DTO:
 * Decouples internal database entities from the REST API presentation layer,
 * preventing circular references and unwanted lazy-loading serialization errors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryDTO {
    private Long id;
    private Long githubRepoId;
    private String name;
    private String fullName;
    private String owner;
    private String description;
    private Integer starsCount;
    private Integer forksCount;
    private Integer openIssuesCount;
    private Integer watchersCount;
    private String defaultBranch;
    private String language;
    private String htmlUrl;
    private Boolean isPrivate;
    private LocalDateTime lastSyncedAt;
}
