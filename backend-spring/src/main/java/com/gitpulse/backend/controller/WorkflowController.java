package com.gitpulse.backend.controller;

import com.gitpulse.backend.model.dto.ReRunResponseDTO;
import com.gitpulse.backend.model.dto.WorkflowRunDTO;
import com.gitpulse.backend.service.GitHubSyncService;
import com.gitpulse.backend.service.TrafficAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller handling CI/CD workflow telemetry and rerun triggers.
 */
@Slf4j
@RestController
@RequestMapping({"/api/v1/workflows", "/api/workflows"})
@RequiredArgsConstructor
public class WorkflowController {

    private final TrafficAnalyticsService trafficAnalyticsService;
    private final GitHubSyncService gitHubSyncService;

    /**
     * GET /api/v1/workflows
     * Returns list of workflow runs, optionally filtered by repository name.
     */
    @GetMapping
    public ResponseEntity<List<WorkflowRunDTO>> getWorkflowRuns(
            @RequestParam(name = "repo", required = false) String repoFullName) {
        log.info("REST request received: GET /api/v1/workflows?repo={}", repoFullName);
        List<WorkflowRunDTO> runs = trafficAnalyticsService.getWorkflowRuns(repoFullName);
        return ResponseEntity.ok(runs);
    }

    /**
     * POST /api/v1/workflows/rerun/{runId}
     * Dispatches a workflow rerun request to GitHub Actions or simulated engine.
     */
    @PostMapping("/rerun/{runId}")
    public ResponseEntity<ReRunResponseDTO> rerunWorkflow(@PathVariable("runId") Long runId) {
        log.info("REST request received: POST /api/v1/workflows/rerun/{}", runId);
        ReRunResponseDTO response = gitHubSyncService.triggerWorkflowRerun(runId);
        return ResponseEntity.ok(response);
    }
}
