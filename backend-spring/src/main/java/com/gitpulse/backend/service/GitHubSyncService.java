package com.gitpulse.backend.service;

import com.gitpulse.backend.exception.ResourceNotFoundException;
import com.gitpulse.backend.model.dto.AuthResponseDTO;
import com.gitpulse.backend.model.dto.LiveEventDTO;
import com.gitpulse.backend.model.dto.ReRunResponseDTO;
import com.gitpulse.backend.model.entity.*;
import com.gitpulse.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service responsible for synchronizing GitHub telemetry into PostgreSQL / H2 database.
 *
 * Why @Transactional:
 * Ensures data integrity across multi-entity sync pipelines (repositories, traffic metrics,
 * workflow runs, and pull requests). If a critical database error occurs mid-sync, changes roll back cleanly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubSyncService {

    private final GitHubAccountRepository accountRepository;
    private final RepositorySummaryRepository repoRepository;
    private final TrafficMetricRepository trafficRepository;
    private final WorkflowRunRepository workflowRepository;
    private final PullRequestSummaryRepository prRepository;
    private final GitHubClientService gitHubClientService;
    private final RealTimeEventsService realTimeEventsService;

    /**
     * Bootstraps initial seed data upon application startup so the developer has immediate insights.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready. Initiating initial GitHub sync bootstrap...");
        try {
            Optional<GitHubAccount> existingAccount = accountRepository.findFirstByOrderByIdAsc();
            if (existingAccount.isEmpty()) {
                log.info("No saved GitHub account found. Performing default initial sync in demo mode.");
                syncAllData("", "octocat-enterprise");
            } else {
                GitHubAccount acc = existingAccount.get();
                log.info("Found existing account '{}'. Running background sync...", acc.getUsername());
                syncAllData(acc.getTokenEncrypted(), acc.getUsername());
            }
        } catch (Exception ex) {
            log.error("Error during initial bootstrap sync: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Connects and registers a GitHub user account.
     */
    @Transactional
    public AuthResponseDTO connectAccount(String token, String username) {
        log.info("Initiating account connection for user '{}'", username != null ? username : "[Auto-Detect]");

        Map<String, Object> userInfo = gitHubClientService.fetchUserInfo(token);
        String resolvedLogin = (String) userInfo.getOrDefault("login", username != null ? username : "octocat-enterprise");
        String avatarUrl = (String) userInfo.getOrDefault("avatar_url", "https://avatars.githubusercontent.com/u/583231?v=4");

        GitHubAccount account = accountRepository.findByUsername(resolvedLogin)
                .orElse(GitHubAccount.builder()
                        .username(resolvedLogin)
                        .createdAt(LocalDateTime.now())
                        .build());

        account.setAvatarUrl(avatarUrl);
        account.setTokenEncrypted(token != null ? token.trim() : "");
        account.setIsTokenValid(true);
        account.setLastSyncedAt(LocalDateTime.now());
        accountRepository.save(account);

        log.info("Successfully persisted GitHubAccount for user '{}'", resolvedLogin);

        // Perform full synchronization in background / transactional flow
        syncAllData(token, resolvedLogin);

        boolean isMock = token == null || token.isBlank();
        return AuthResponseDTO.builder()
                .success(true)
                .username(resolvedLogin)
                .avatarUrl(avatarUrl)
                .message("Successfully connected and synchronized account: " + resolvedLogin)
                .isMockMode(isMock)
                .build();
    }

    /**
     * Checks current connection status.
     */
    public AuthResponseDTO getAccountStatus() {
        Optional<GitHubAccount> accountOpt = accountRepository.findFirstByOrderByIdAsc();
        if (accountOpt.isPresent()) {
            GitHubAccount acc = accountOpt.get();
            boolean isMock = acc.getTokenEncrypted() == null || acc.getTokenEncrypted().isBlank();
            return AuthResponseDTO.builder()
                    .success(true)
                    .username(acc.getUsername())
                    .avatarUrl(acc.getAvatarUrl())
                    .message("Connected as " + acc.getUsername())
                    .isMockMode(isMock)
                    .build();
        }

        return AuthResponseDTO.builder()
                .success(false)
                .username(null)
                .avatarUrl(null)
                .message("No GitHub account connected")
                .isMockMode(true)
                .build();
    }

    /**
     * Detects locally configured GitHub CLI token and connects automatically.
     */
    @Transactional
    public AuthResponseDTO detectCliAuth() {
        log.info("Attempting automated CLI token discovery...");
        Optional<String> cliToken = gitHubClientService.detectCliAuthToken();
        if (cliToken.isPresent()) {
            log.info("Local CLI token found. Connecting account automatically.");
            return connectAccount(cliToken.get(), null);
        }

        log.info("No local CLI token detected. Falling back to default mock workspace.");
        return connectAccount("", "octocat-enterprise");
    }

    /**
     * Primary data synchronization pipeline.
     */
    @Transactional
    public void syncAllData(String token, String username) {
        log.info("Starting complete telemetry synchronization for user: '{}'", username);

        List<Map<String, Object>> repoMaps = gitHubClientService.fetchUserRepositories(token, username);
        log.info("Discovered {} repositories from GitHub API", repoMaps.size());

        for (Map<String, Object> map : repoMaps) {
            try {
                syncSingleRepository(token, map);
            } catch (Exception ex) {
                log.error("Failed to sync repository data for payload {}: {}", map.get("name"), ex.getMessage(), ex);
            }
        }

        // Broadcast real-time SSE event to notify React dashboard of updated metrics
        realTimeEventsService.broadcastEvent(LiveEventDTO.builder()
                .eventType("SYNC_COMPLETED")
                .message("Full GitHub repository and CI/CD telemetry synchronization completed")
                .payload(Map.of("syncedRepos", repoMaps.size(), "syncedAt", LocalDateTime.now().toString()))
                .timestamp(LocalDateTime.now())
                .build());

        log.info("Completed complete telemetry synchronization for user: '{}'", username);
    }

    private void syncSingleRepository(String token, Map<String, Object> map) {
        Long repoId = ((Number) map.get("id")).longValue();
        String name = (String) map.get("name");
        String fullName = (String) map.get("full_name");
        String description = (String) map.get("description");
        int stars = map.containsKey("stargazers_count") ? ((Number) map.get("stargazers_count")).intValue() : 0;
        int forks = map.containsKey("forks_count") ? ((Number) map.get("forks_count")).intValue() : 0;
        int openIssues = map.containsKey("open_issues_count") ? ((Number) map.get("open_issues_count")).intValue() : 0;
        int watchers = map.containsKey("watchers_count") ? ((Number) map.get("watchers_count")).intValue() : stars;
        String defaultBranch = (String) map.getOrDefault("default_branch", "main");
        String language = (String) map.getOrDefault("language", "TypeScript");
        String htmlUrl = (String) map.getOrDefault("html_url", "https://github.com/" + fullName);
        boolean isPrivate = Boolean.TRUE.equals(map.get("private"));

        @SuppressWarnings("unchecked")
        Map<String, Object> ownerMap = (Map<String, Object>) map.get("owner");
        String owner = (ownerMap != null && ownerMap.containsKey("login"))
                ? (String) ownerMap.get("login")
                : fullName.split("/")[0];

        RepositorySummary repo = repoRepository.findByFullName(fullName)
                .orElse(RepositorySummary.builder()
                        .fullName(fullName)
                        .name(name)
                        .owner(owner)
                        .build());

        repo.setGithubRepoId(repoId);
        repo.setName(name);
        repo.setFullName(fullName);
        repo.setOwner(owner);
        repo.setDescription(description);
        repo.setStarsCount(stars);
        repo.setForksCount(forks);
        repo.setOpenIssuesCount(openIssues);
        repo.setWatchersCount(watchers);
        repo.setDefaultBranch(defaultBranch);
        repo.setLanguage(language);
        repo.setHtmlUrl(htmlUrl);
        repo.setIsPrivate(isPrivate);
        repo.setLastSyncedAt(LocalDateTime.now());

        repo = repoRepository.save(repo);
        log.info("Synchronized repository record: {}", fullName);

        // 1. Sync Traffic Metrics
        syncTraffic(token, owner, name, repo);

        // 2. Sync CI/CD Workflow Runs
        syncWorkflowRuns(token, owner, name, repo);

        // 3. Sync Pull Requests
        syncPullRequests(token, owner, name, repo);
    }

    private void syncTraffic(String token, String owner, String repoName, RepositorySummary repo) {
        Map<String, Object> trafficData = gitHubClientService.fetchRepositoryTraffic(token, owner, repoName);
        if (trafficData == null) return;

        @SuppressWarnings("unchecked")
        Map<String, Object> viewsMap = (Map<String, Object>) trafficData.get("views");
        if (viewsMap != null && viewsMap.containsKey("views")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> viewsList = (List<Map<String, Object>>) viewsMap.get("views");
            for (Map<String, Object> item : viewsList) {
                String ts = (String) item.get("timestamp");
                LocalDate date = LocalDate.parse(ts.substring(0, 10));
                int count = ((Number) item.get("count")).intValue();
                int uniques = ((Number) item.get("uniques")).intValue();

                TrafficMetric metric = trafficRepository.findByRepositoryAndDate(repo, date)
                        .orElse(TrafficMetric.builder()
                                .repository(repo)
                                .date(date)
                                .build());

                metric.setViewsCount(count);
                metric.setUniqueVisitors(uniques);
                trafficRepository.save(metric);
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> clonesMap = (Map<String, Object>) trafficData.get("clones");
        if (clonesMap != null && clonesMap.containsKey("clones")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> clonesList = (List<Map<String, Object>>) clonesMap.get("clones");
            for (Map<String, Object> item : clonesList) {
                String ts = (String) item.get("timestamp");
                LocalDate date = LocalDate.parse(ts.substring(0, 10));
                int count = ((Number) item.get("count")).intValue();
                int uniques = ((Number) item.get("uniques")).intValue();

                TrafficMetric metric = trafficRepository.findByRepositoryAndDate(repo, date)
                        .orElse(TrafficMetric.builder()
                                .repository(repo)
                                .date(date)
                                .build());

                metric.setClonesCount(count);
                metric.setUniqueCloners(uniques);
                trafficRepository.save(metric);
            }
        }
    }

    private void syncWorkflowRuns(String token, String owner, String repoName, RepositorySummary repo) {
        List<Map<String, Object>> runMaps = gitHubClientService.fetchWorkflowRuns(token, owner, repoName);
        for (Map<String, Object> map : runMaps) {
            Long runId = ((Number) map.get("id")).longValue();
            String name = (String) map.getOrDefault("name", "CI/CD Build");
            String event = (String) map.getOrDefault("event", "push");
            String status = (String) map.getOrDefault("status", "completed");
            String conclusion = (String) map.get("conclusion");
            String branch = (String) map.getOrDefault("head_branch", "main");
            String sha = (String) map.getOrDefault("head_sha", "HEAD");
            String htmlUrl = (String) map.getOrDefault("html_url", "https://github.com/actions/runs/" + runId);

            String commitMsg = "Commit update";
            if (map.containsKey("head_commit") && map.get("head_commit") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> commit = (Map<String, Object>) map.get("head_commit");
                commitMsg = (String) commit.getOrDefault("message", commitMsg);
            }

            String author = "developer";
            if (map.containsKey("actor") && map.get("actor") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> actor = (Map<String, Object>) map.get("actor");
                author = (String) actor.getOrDefault("login", author);
            }

            Long duration = map.containsKey("duration_seconds") ? ((Number) map.get("duration_seconds")).longValue() : 95L;

            WorkflowRun run = workflowRepository.findByGithubRunId(runId)
                    .orElse(WorkflowRun.builder()
                            .githubRunId(runId)
                            .repository(repo)
                            .build());

            run.setWorkflowName(name);
            run.setEvent(event);
            run.setStatus(status);
            run.setConclusion(conclusion);
            run.setCommitMessage(commitMsg);
            run.setCommitSha(sha != null && sha.length() > 8 ? sha.substring(0, 8) : sha);
            run.setBranch(branch);
            run.setAuthor(author);
            run.setDurationSeconds(duration);
            run.setHtmlUrl(htmlUrl);

            workflowRepository.save(run);
        }
    }

    private void syncPullRequests(String token, String owner, String repoName, RepositorySummary repo) {
        List<Map<String, Object>> prMaps = gitHubClientService.fetchPullRequests(token, owner, repoName);
        for (Map<String, Object> map : prMaps) {
            Long prId = ((Number) map.get("id")).longValue();
            int prNumber = ((Number) map.get("number")).intValue();
            String title = (String) map.getOrDefault("title", "Pull Request");
            String state = (String) map.getOrDefault("state", "open");
            boolean isDraft = Boolean.TRUE.equals(map.get("draft"));
            int additions = map.containsKey("additions") ? ((Number) map.get("additions")).intValue() : 50;
            int deletions = map.containsKey("deletions") ? ((Number) map.get("deletions")).intValue() : 10;
            int comments = map.containsKey("comments") ? ((Number) map.get("comments")).intValue() : 0;
            String htmlUrl = (String) map.getOrDefault("html_url", "https://github.com/pulls/" + prNumber);

            String author = "contributor";
            if (map.containsKey("user") && map.get("user") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> user = (Map<String, Object>) map.get("user");
                author = (String) user.getOrDefault("login", author);
            }

            PullRequestSummary pr = prRepository.findByGithubPrId(prId)
                    .orElse(PullRequestSummary.builder()
                            .githubPrId(prId)
                            .repository(repo)
                            .prNumber(prNumber)
                            .build());

            pr.setTitle(title);
            pr.setAuthor(author);
            pr.setState(state);
            pr.setIsDraft(isDraft);
            pr.setDraft(isDraft);
            pr.setAdditions(additions);
            pr.setDeletions(deletions);
            pr.setCommentsCount(comments);
            pr.setHtmlUrl(htmlUrl);

            prRepository.save(pr);
        }
    }

    /**
     * Triggers a workflow rerun and broadcasts state changes to SSE subscribers.
     */
    @Transactional
    public ReRunResponseDTO triggerWorkflowRerun(Long runId) {
        log.info("Rerun requested for workflow run ID: {}", runId);

        WorkflowRun run = workflowRepository.findById(runId)
                .or(() -> workflowRepository.findByGithubRunId(runId))
                .orElseThrow(() -> new ResourceNotFoundException("Workflow run not found with ID: " + runId));

        RepositorySummary repo = run.getRepository();
        String owner = repo != null ? repo.getOwner() : "octocat-enterprise";
        String repoName = repo != null ? repo.getName() : "gitpulse-core";

        Optional<GitHubAccount> acc = accountRepository.findFirstByOrderByIdAsc();
        String token = acc.map(GitHubAccount::getTokenEncrypted).orElse("");

        boolean dispatched = gitHubClientService.triggerWorkflowReRun(token, owner, repoName, run.getGithubRunId());

        if (dispatched) {
            run.setStatus("in_progress");
            run.setConclusion(null);
            run.setUpdatedAt(LocalDateTime.now());
            workflowRepository.save(run);

            // Broadcast SSE update
            realTimeEventsService.broadcastEvent(LiveEventDTO.builder()
                    .eventType("WORKFLOW_STATUS")
                    .message("Workflow run #" + run.getGithubRunId() + " rerun triggered successfully")
                    .payload(Map.of(
                            "runId", run.getId(),
                            "githubRunId", run.getGithubRunId(),
                            "status", "in_progress",
                            "workflowName", run.getWorkflowName()
                    ))
                    .timestamp(LocalDateTime.now())
                    .build());

            return ReRunResponseDTO.builder()
                    .success(true)
                    .runId(run.getGithubRunId())
                    .status("in_progress")
                    .message("Rerun dispatched successfully for " + run.getWorkflowName())
                    .build();
        } else {
            return ReRunResponseDTO.builder()
                    .success(false)
                    .runId(run.getGithubRunId())
                    .status(run.getStatus())
                    .message("Failed to dispatch workflow rerun upstream")
                    .build();
        }
    }

    /**
     * Scheduled periodic background synchronization.
     */
    @Scheduled(fixedRateString = "${gitpulse.github.sync-interval-seconds:300}000")
    public void scheduledSync() {
        Optional<GitHubAccount> accountOpt = accountRepository.findFirstByOrderByIdAsc();
        if (accountOpt.isPresent()) {
            GitHubAccount acc = accountOpt.get();
            log.info("Executing periodic background telemetry sync for user: '{}'", acc.getUsername());
            syncAllData(acc.getTokenEncrypted(), acc.getUsername());
        }
    }
}
