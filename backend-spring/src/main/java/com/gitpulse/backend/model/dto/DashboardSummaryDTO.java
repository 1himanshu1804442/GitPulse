package com.gitpulse.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * High-level executive dashboard summary aggregation DTO.
 *
 * Why composite DTO:
 * Supplies all top-level telemetry metrics in a single network round-trip,
 * optimizing React client load time and initial render speed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {

    private int totalRepositories;
    private int totalStars;
    private int totalForks;
    private int totalOpenIssues;
    private int totalViews14d;
    private int totalClones14d;
    private long totalWorkflowsRun;
    private long activeWorkflowsCount;
    private long pendingReviewPrsCount;

    @Builder.Default
    private Map<String, Integer> topLanguages = new HashMap<>();

    @Builder.Default
    private List<WorkflowRunDTO> recentRuns = new ArrayList<>();

    @Builder.Default
    private List<PullRequestDTO> activePrs = new ArrayList<>();
}
