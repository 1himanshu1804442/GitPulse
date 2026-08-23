package com.gitpulse.backend.service;

import com.gitpulse.backend.exception.ResourceNotFoundException;
import com.gitpulse.backend.model.dto.*;
import com.gitpulse.backend.model.entity.PullRequestSummary;
import com.gitpulse.backend.model.entity.RepositorySummary;
import com.gitpulse.backend.model.entity.TrafficMetric;
import com.gitpulse.backend.model.entity.WorkflowRun;
import com.gitpulse.backend.repository.PullRequestSummaryRepository;
import com.gitpulse.backend.repository.RepositorySummaryRepository;
import com.gitpulse.backend.repository.TrafficMetricRepository;
import com.gitpulse.backend.repository.WorkflowRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for aggregating repository metrics, traffic velocity,
 * CI/CD telemetry, and executive dashboard summaries.
 *
 * Why Service layer aggregation:
 * Offloads heavy mathematical calculations and DTO transformations from the Controllers,
 * ensuring high performance, clean testability, and strict adherence to layered architecture.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrafficAnalyticsService {

    private final RepositorySummaryRepository repoRepository;
    private final TrafficMetricRepository trafficRepository;
    private final WorkflowRunRepository workflowRepository;
    private final PullRequestSummaryRepository prRepository;

    /**
     * Aggregates all top-level metrics for the executive dashboard overview.
     */
    public DashboardSummaryDTO getOverallDashboardSummary() {
        log.info("Calculating overall dashboard telemetry summary");

        List<RepositorySummary> repos = repoRepository.findAllByOrderByStarsCountDesc();

        int totalStars = 0;
        int totalForks = 0;
        int totalOpenIssues = 0;
        Map<String, Integer> languageDistribution = new HashMap<>();

        for (RepositorySummary repo : repos) {
            totalStars += repo.getStarsCount() != null ? repo.getStarsCount() : 0;
            totalForks += repo.getForksCount() != null ? repo.getForksCount() : 0;
            totalOpenIssues += repo.getOpenIssuesCount() != null ? repo.getOpenIssuesCount() : 0;

            String lang = repo.getLanguage();
            if (lang != null && !lang.isBlank()) {
                languageDistribution.put(lang, languageDistribution.getOrDefault(lang, 0) + 1);
            }
        }

        // 14-day traffic calculation
        LocalDate startDate = LocalDate.now().minusDays(14);
        int totalViews14d = 0;
        int totalClones14d = 0;

        List<TrafficMetric> allMetrics = trafficRepository.findAll();
        for (TrafficMetric metric : allMetrics) {
            if (metric.getDate() != null && !metric.getDate().isBefore(startDate)) {
                totalViews14d += metric.getViewsCount() != null ? metric.getViewsCount() : 0;
                totalClones14d += metric.getClonesCount() != null ? metric.getClonesCount() : 0;
            }
        }

        long totalWorkflows = workflowRepository.count();
        long inProgressCount = workflowRepository.countByStatus("in_progress");
        long queuedCount = workflowRepository.countByStatus("queued");
        long pendingReviewCount = prRepository.countByState("open");

        List<WorkflowRunDTO> recentRuns = workflowRepository.findTop20ByOrderByCreatedAtDesc()
                .stream()
                .limit(10)
                .map(this::mapWorkflowRunToDTO)
                .collect(Collectors.toList());

        List<PullRequestDTO> activePrs = prRepository.findByStateOrderByCreatedAtDesc("open")
                .stream()
                .limit(10)
                .map(this::mapPrToDTO)
                .collect(Collectors.toList());

        return DashboardSummaryDTO.builder()
                .totalRepositories(repos.size())
                .totalStars(totalStars)
                .totalForks(totalForks)
                .totalOpenIssues(totalOpenIssues)
                .totalViews14d(totalViews14d)
                .totalClones14d(totalClones14d)
                .totalWorkflowsRun(totalWorkflows)
                .activeWorkflowsCount(inProgressCount + queuedCount)
                .pendingReviewPrsCount(pendingReviewCount)
                .topLanguages(languageDistribution)
                .recentRuns(recentRuns)
                .activePrs(activePrs)
                .build();
    }

    /**
     * Retrieves daily traffic analytics and growth velocity for a specific repository or overall.
     */
    public TrafficAnalyticsDTO getRepositoryTraffic(String repoFullName) {
        log.info("Generating traffic analytics report for repository: '{}'", repoFullName != null ? repoFullName : "ALL");

        List<TrafficMetric> metrics;
        String targetName = repoFullName;

        if (repoFullName != null && !repoFullName.isBlank()) {
            RepositorySummary repo = repoRepository.findByFullName(repoFullName)
                    .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + repoFullName));
            metrics = trafficRepository.findByRepositoryOrderByDateAsc(repo);
        } else {
            metrics = trafficRepository.findAll();
            targetName = "All Repositories (Aggregated)";
        }

        // Aggregate daily data
        Map<LocalDate, TrafficAnalyticsDTO.DailyTrafficPointDTO> dailyMap = new TreeMap<>();
        int totalViews = 0;
        int totalUniqueVisitors = 0;
        int totalClones = 0;
        int totalUniqueCloners = 0;

        for (TrafficMetric m : metrics) {
            LocalDate date = m.getDate();
            if (date == null) continue;

            int views = m.getViewsCount() != null ? m.getViewsCount() : 0;
            int uniques = m.getUniqueVisitors() != null ? m.getUniqueVisitors() : 0;
            int clones = m.getClonesCount() != null ? m.getClonesCount() : 0;
            int cloners = m.getUniqueCloners() != null ? m.getUniqueCloners() : 0;

            totalViews += views;
            totalUniqueVisitors += uniques;
            totalClones += clones;
            totalUniqueCloners += cloners;

            TrafficAnalyticsDTO.DailyTrafficPointDTO point = dailyMap.computeIfAbsent(date, d ->
                    TrafficAnalyticsDTO.DailyTrafficPointDTO.builder()
                            .date(d)
                            .views(0)
                            .uniqueVisitors(0)
                            .clones(0)
                            .uniqueCloners(0)
                            .build()
            );

            point.setViews(point.getViews() + views);
            point.setUniqueVisitors(point.getUniqueVisitors() + uniques);
            point.setClones(point.getClones() + clones);
            point.setUniqueCloners(point.getUniqueCloners() + cloners);
        }

        // Calculate velocity score (daily views momentum index)
        double velocityScore = dailyMap.isEmpty() ? 0.0 : Math.round((totalViews / (double) Math.max(1, dailyMap.size())) * 100.0) / 100.0;

        return TrafficAnalyticsDTO.builder()
                .repoFullName(targetName)
                .totalViews(totalViews)
                .totalUniqueVisitors(totalUniqueVisitors)
                .totalClones(totalClones)
                .totalUniqueCloners(totalUniqueCloners)
                .velocityScore(velocityScore)
                .dailyPoints(new ArrayList<>(dailyMap.values()))
                .build();
    }

    /**
     * Retrieves all synchronized repositories sorted by popularity (stars).
     */
    public List<RepositoryDTO> getAllRepositories() {
        log.info("Fetching repository catalog sorted by stars");
        return repoRepository.findAllByOrderByStarsCountDesc()
                .stream()
                .map(this::mapRepoToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves CI/CD workflow runs optionally filtered by repository name.
     */
    public List<WorkflowRunDTO> getWorkflowRuns(String repoFullName) {
        log.info("Fetching workflow runs. Filter repo: {}", repoFullName);
        if (repoFullName != null && !repoFullName.isBlank()) {
            RepositorySummary repo = repoRepository.findByFullName(repoFullName)
                    .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + repoFullName));
            return workflowRepository.findByRepositoryOrderByCreatedAtDesc(repo)
                    .stream()
                    .map(this::mapWorkflowRunToDTO)
                    .collect(Collectors.toList());
        }

        return workflowRepository.findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapWorkflowRunToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves pull requests optionally filtered by repository.
     */
    public List<PullRequestDTO> getPullRequests(String repoFullName) {
        log.info("Fetching pull requests. Filter repo: {}", repoFullName);
        if (repoFullName != null && !repoFullName.isBlank()) {
            RepositorySummary repo = repoRepository.findByFullName(repoFullName)
                    .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + repoFullName));
            return prRepository.findByRepositoryOrderByCreatedAtDesc(repo)
                    .stream()
                    .map(this::mapPrToDTO)
                    .collect(Collectors.toList());
        }

        return prRepository.findAll()
                .stream()
                .map(this::mapPrToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves code review queue consisting of all open pull requests.
     */
    public List<PullRequestDTO> getReviewQueue() {
        log.info("Fetching active pull request review queue");
        return prRepository.findByStateOrderByCreatedAtDesc("open")
                .stream()
                .map(this::mapPrToDTO)
                .collect(Collectors.toList());
    }

    // Mapping Helpers
    public RepositoryDTO mapRepoToDTO(RepositorySummary repo) {
        return RepositoryDTO.builder()
                .id(repo.getId())
                .githubRepoId(repo.getGithubRepoId())
                .name(repo.getName())
                .fullName(repo.getFullName())
                .owner(repo.getOwner())
                .description(repo.getDescription())
                .starsCount(repo.getStarsCount())
                .forksCount(repo.getForksCount())
                .openIssuesCount(repo.getOpenIssuesCount())
                .watchersCount(repo.getWatchersCount())
                .defaultBranch(repo.getDefaultBranch())
                .language(repo.getLanguage())
                .htmlUrl(repo.getHtmlUrl())
                .isPrivate(repo.getIsPrivate())
                .lastSyncedAt(repo.getLastSyncedAt())
                .build();
    }

    public WorkflowRunDTO mapWorkflowRunToDTO(WorkflowRun run) {
        String repoName = run.getRepository() != null ? run.getRepository().getFullName() : "gitpulse/core";
        return WorkflowRunDTO.builder()
                .id(run.getId())
                .githubRunId(run.getGithubRunId())
                .repoFullName(repoName)
                .workflowName(run.getWorkflowName())
                .event(run.getEvent())
                .status(run.getStatus())
                .conclusion(run.getConclusion())
                .commitMessage(run.getCommitMessage())
                .commitSha(run.getCommitSha())
                .branch(run.getBranch())
                .author(run.getAuthor())
                .durationSeconds(run.getDurationSeconds())
                .htmlUrl(run.getHtmlUrl())
                .createdAt(run.getCreatedAt())
                .updatedAt(run.getUpdatedAt())
                .build();
    }

    public PullRequestDTO mapPrToDTO(PullRequestSummary pr) {
        String repoName = pr.getRepository() != null ? pr.getRepository().getFullName() : "gitpulse/core";
        return PullRequestDTO.builder()
                .id(pr.getId())
                .githubPrId(pr.getGithubPrId())
                .repoFullName(repoName)
                .prNumber(pr.getPrNumber())
                .title(pr.getTitle())
                .author(pr.getAuthor())
                .state(pr.getState())
                .isDraft(pr.getIsDraft())
                .draft(pr.getDraft())
                .additions(pr.getAdditions())
                .deletions(pr.getDeletions())
                .commentsCount(pr.getCommentsCount())
                .htmlUrl(pr.getHtmlUrl())
                .createdAt(pr.getCreatedAt())
                .updatedAt(pr.getUpdatedAt())
                .build();
    }
}
