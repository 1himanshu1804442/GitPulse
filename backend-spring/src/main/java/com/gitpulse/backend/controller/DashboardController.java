package com.gitpulse.backend.controller;

import com.gitpulse.backend.model.dto.DashboardSummaryDTO;
import com.gitpulse.backend.model.dto.RepositoryDTO;
import com.gitpulse.backend.model.dto.TrafficAnalyticsDTO;
import com.gitpulse.backend.service.TrafficAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller handling high-level overview metrics, repository listings, and traffic analytics.
 *
 * Why Layered Architecture:
 * The controller is strictly responsible for handling HTTP routing, query parameters,
 * and HTTP response status codes. All business logic and calculations reside in TrafficAnalyticsService.
 */
@Slf4j
@RestController
@RequestMapping({"/api/v1", "/api"})
@RequiredArgsConstructor
public class DashboardController {

    private final TrafficAnalyticsService trafficAnalyticsService;

    /**
     * GET /api/v1/dashboard/summary
     * Returns top-level aggregated statistics (stars, forks, open issues, 14-day views, CI/CD health).
     */
    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary() {
        log.info("REST request received: GET /dashboard/summary");
        DashboardSummaryDTO summary = trafficAnalyticsService.getOverallDashboardSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/v1/repos or /api/v1/repositories
     * Returns all synchronized repositories sorted by popularity.
     */
    @GetMapping({"/repos", "/repositories"})
    public ResponseEntity<List<RepositoryDTO>> getAllRepositories() {
        log.info("REST request received: GET /repos");
        List<RepositoryDTO> repos = trafficAnalyticsService.getAllRepositories();
        return ResponseEntity.ok(repos);
    }

    /**
     * GET /api/v1/traffic or /api/v1/analytics/traffic
     * Returns 14-day views, clones, and visitor velocity trends for a given repository or overall.
     */
    @GetMapping({"/traffic", "/analytics/traffic"})
    public ResponseEntity<TrafficAnalyticsDTO> getTrafficAnalytics(
            @RequestParam(name = "repo", required = false) String repoFullName) {
        log.info("REST request received: GET /traffic?repo={}", repoFullName);
        TrafficAnalyticsDTO traffic = trafficAnalyticsService.getRepositoryTraffic(repoFullName);
        return ResponseEntity.ok(traffic);
    }
}
