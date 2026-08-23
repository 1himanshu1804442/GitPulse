package com.gitpulse.backend.controller;

import com.gitpulse.backend.model.dto.PullRequestDTO;
import com.gitpulse.backend.service.TrafficAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller handling GitHub Pull Request telemetry and review queue workloads.
 */
@Slf4j
@RestController
@RequestMapping({"/api/v1/pull-requests", "/api/pull-requests"})
@RequiredArgsConstructor
public class PullRequestController {

    private final TrafficAnalyticsService trafficAnalyticsService;

    /**
     * GET /api/v1/pull-requests
     * Returns list of pull requests, optionally filtered by repository name.
     */
    @GetMapping
    public ResponseEntity<List<PullRequestDTO>> getPullRequests(
            @RequestParam(name = "repo", required = false) String repoFullName) {
        log.info("REST request received: GET /api/v1/pull-requests?repo={}", repoFullName);
        List<PullRequestDTO> prs = trafficAnalyticsService.getPullRequests(repoFullName);
        return ResponseEntity.ok(prs);
    }

    /**
     * GET /api/v1/pull-requests/review-queue
     * Returns actionable review queue consisting of open pull requests.
     */
    @GetMapping("/review-queue")
    public ResponseEntity<List<PullRequestDTO>> getReviewQueue() {
        log.info("REST request received: GET /api/v1/pull-requests/review-queue");
        List<PullRequestDTO> queue = trafficAnalyticsService.getReviewQueue();
        return ResponseEntity.ok(queue);
    }
}
