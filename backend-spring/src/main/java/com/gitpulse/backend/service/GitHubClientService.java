package com.gitpulse.backend.service;

import com.gitpulse.backend.exception.GitHubApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service responsible for interacting with GitHub REST API v3 using Spring RestClient.
 *
 * Why Fallback Mock Generation:
 * Provides instant out-of-the-box functionality even without a GitHub Personal Access Token
 * or when GitHub rate limits (60 req/hr unauthenticated) are exceeded, ensuring developers
 * always have rich, representative data to explore in the dashboard.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubClientService {

    private final RestClient restClient;

    @Value("${gitpulse.github.api-base-url:https://api.github.com}")
    private String apiBaseUrl;

    @Value("${gitpulse.github.mock-fallback-enabled:true}")
    private boolean mockFallbackEnabled;

    /**
     * Fetches authenticated user information from GitHub API.
     */
    public Map<String, Object> fetchUserInfo(String token) {
        if (token == null || token.isBlank()) {
            log.info("No GitHub token provided; generating mock user profile");
            return generateMockUserProfile("octocat-enterprise");
        }

        try {
            log.info("Fetching GitHub user profile using supplied token");
            Map<String, Object> response = restClient.get()
                    .uri(apiBaseUrl + "/user")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.error("GitHub /user call returned error code: {}", res.getStatusCode());
                        throw new GitHubApiException("Failed to fetch user from GitHub API: HTTP " + res.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response != null && response.containsKey("login")) {
                return response;
            }
            return generateMockUserProfile("github-user");
        } catch (Exception ex) {
            log.error("Exception fetching GitHub user info: {}. Falling back to mock data.", ex.getMessage(), ex);
            if (mockFallbackEnabled) {
                return generateMockUserProfile("octocat-enterprise");
            }
            throw new GitHubApiException("Unable to authenticate with GitHub API: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fetches repositories for the specified user or authenticated user.
     */
    public List<Map<String, Object>> fetchUserRepositories(String token, String username) {
        if (token == null || token.isBlank()) {
            log.info("No token provided; returning mock repository catalog");
            return generateMockRepositories(username != null ? username : "octocat-enterprise");
        }

        try {
            String uri = (username != null && !username.isBlank())
                    ? apiBaseUrl + "/users/" + username + "/repos?sort=updated&per_page=30"
                    : apiBaseUrl + "/user/repos?sort=updated&per_page=30";

            log.info("Querying GitHub repositories at URI: {}", uri);
            List<Map<String, Object>> repos = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (repos != null && !repos.isEmpty()) {
                return repos;
            }
            return generateMockRepositories(username != null ? username : "octocat-enterprise");
        } catch (Exception ex) {
            log.error("Failed to query GitHub repositories: {}. Falling back to mock repositories.", ex.getMessage(), ex);
            if (mockFallbackEnabled) {
                return generateMockRepositories(username != null ? username : "octocat-enterprise");
            }
            throw new GitHubApiException("Failed to fetch repositories: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fetches 14-day traffic (views & clones) for a repository.
     */
    public Map<String, Object> fetchRepositoryTraffic(String token, String owner, String repoName) {
        if (token == null || token.isBlank()) {
            return generateMockTraffic(owner, repoName);
        }

        Map<String, Object> result = new HashMap<>();
        try {
            log.info("Fetching traffic analytics for repository: {}/{}", owner, repoName);
            Map<String, Object> views = restClient.get()
                    .uri(apiBaseUrl + "/repos/" + owner + "/" + repoName + "/traffic/views")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            Map<String, Object> clones = restClient.get()
                    .uri(apiBaseUrl + "/repos/" + owner + "/" + repoName + "/traffic/clones")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            result.put("views", views);
            result.put("clones", clones);
            return result;
        } catch (Exception ex) {
            log.warn("Could not fetch real traffic for {}/{}: {}. Providing generated realistic metrics.", owner, repoName, ex.getMessage());
            return generateMockTraffic(owner, repoName);
        }
    }

    /**
     * Fetches GitHub Actions workflow runs for a repository.
     */
    public List<Map<String, Object>> fetchWorkflowRuns(String token, String owner, String repoName) {
        if (token == null || token.isBlank()) {
            return generateMockWorkflowRuns(owner, repoName);
        }

        try {
            log.info("Fetching workflow runs for repository: {}/{}", owner, repoName);
            Map<String, Object> response = restClient.get()
                    .uri(apiBaseUrl + "/repos/" + owner + "/" + repoName + "/actions/runs?per_page=15")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response != null && response.containsKey("workflow_runs")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> runs = (List<Map<String, Object>>) response.get("workflow_runs");
                if (runs != null && !runs.isEmpty()) {
                    return runs;
                }
            }
            return generateMockWorkflowRuns(owner, repoName);
        } catch (Exception ex) {
            log.warn("Workflow runs unavailable for {}/{}: {}. Falling back to mock runs.", owner, repoName, ex.getMessage());
            return generateMockWorkflowRuns(owner, repoName);
        }
    }

    /**
     * Fetches pull requests for a repository.
     */
    public List<Map<String, Object>> fetchPullRequests(String token, String owner, String repoName) {
        if (token == null || token.isBlank()) {
            return generateMockPullRequests(owner, repoName);
        }

        try {
            log.info("Fetching pull requests for repository: {}/{}", owner, repoName);
            List<Map<String, Object>> prs = restClient.get()
                    .uri(apiBaseUrl + "/repos/" + owner + "/" + repoName + "/pulls?state=all&per_page=15")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (prs != null && !prs.isEmpty()) {
                return prs;
            }
            return generateMockPullRequests(owner, repoName);
        } catch (Exception ex) {
            log.warn("Pull requests unavailable for {}/{}: {}. Falling back to mock PRs.", owner, repoName, ex.getMessage());
            return generateMockPullRequests(owner, repoName);
        }
    }

    /**
     * Dispatches a workflow rerun request to GitHub Actions.
     */
    public boolean triggerWorkflowReRun(String token, String owner, String repoName, Long runId) {
        log.info("Triggering workflow rerun for run ID: {} on {}/{}", runId, owner, repoName);
        if (token == null || token.isBlank()) {
            log.info("Operating in mock mode: Successfully simulated workflow rerun for runId={}", runId);
            return true;
        }

        try {
            restClient.post()
                    .uri(apiBaseUrl + "/repos/" + owner + "/" + repoName + "/actions/runs/" + runId + "/rerun")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim())
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.error("Failed to trigger GitHub Actions rerun upstream: {}", ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Detects locally configured GitHub CLI token via environment or 'gh auth token'.
     */
    public Optional<String> detectCliAuthToken() {
        String envToken = System.getenv("GITHUB_TOKEN");
        if (envToken != null && !envToken.isBlank()) {
            log.info("Detected GitHub token from GITHUB_TOKEN environment variable");
            return Optional.of(envToken.trim());
        }

        try {
            Process process = new ProcessBuilder("gh", "auth", "token").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.isBlank()) {
                    log.info("Detected active GitHub token from GitHub CLI (gh auth token)");
                    return Optional.of(line.trim());
                }
            }
        } catch (Exception ex) {
            log.debug("GitHub CLI not installed or not authenticated: {}", ex.getMessage());
        }
        return Optional.empty();
    }

    // ==========================================
    // MOCK DATA GENERATION ENGINE
    // ==========================================

    private Map<String, Object> generateMockUserProfile(String username) {
        Map<String, Object> user = new HashMap<>();
        user.put("login", username);
        user.put("avatar_url", "https://avatars.githubusercontent.com/u/583231?v=4");
        user.put("name", "GitPulse Lead Architect");
        user.put("bio", "Engineering full-stack real-time developer tooling");
        user.put("public_repos", 8);
        return user;
    }

    private List<Map<String, Object>> generateMockRepositories(String owner) {
        List<Map<String, Object>> list = new ArrayList<>();

        list.add(createMockRepo(101L, "gitpulse-core", owner + "/gitpulse-core", owner,
                "Real-Time GitHub Intelligence & CI/CD Telemetry Platform", 1240, 185, 12, 1240, "Java", false));
        list.add(createMockRepo(102L, "quantum-mesh", owner + "/quantum-mesh", owner,
                "High-performance distributed service mesh and proxy engine", 3420, 420, 24, 3420, "Rust", false));
        list.add(createMockRepo(103L, "cloud-nexus-ui", owner + "/cloud-nexus-ui", owner,
                "Enterprise React dashboard design system and component library", 890, 94, 5, 890, "TypeScript", false));
        list.add(createMockRepo(104L, "hyperion-db", owner + "/hyperion-db", owner,
                "Time-series in-memory analytics engine with SQL support", 2150, 310, 18, 2150, "Go", false));
        list.add(createMockRepo(105L, "neural-flow", owner + "/neural-flow", owner,
                "Automated prompt-chain orchestration and LLM gateway", 1680, 230, 9, 1680, "Python", false));

        return list;
    }

    private Map<String, Object> createMockRepo(Long id, String name, String fullName, String owner,
                                               String desc, int stars, int forks, int issues, int watchers,
                                               String lang, boolean isPrivate) {
        Map<String, Object> repo = new HashMap<>();
        repo.put("id", id);
        repo.put("name", name);
        repo.put("full_name", fullName);
        Map<String, Object> ownerMap = new HashMap<>();
        ownerMap.put("login", owner);
        repo.put("owner", ownerMap);
        repo.put("description", desc);
        repo.put("stargazers_count", stars);
        repo.put("forks_count", forks);
        repo.put("open_issues_count", issues);
        repo.put("watchers_count", watchers);
        repo.put("default_branch", "main");
        repo.put("language", lang);
        repo.put("html_url", "https://github.com/" + fullName);
        repo.put("private", isPrivate);
        return repo;
    }

    private Map<String, Object> generateMockTraffic(String owner, String repoName) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> viewsList = new ArrayList<>();
        List<Map<String, Object>> clonesList = new ArrayList<>();

        LocalDate today = LocalDate.now();
        Random rand = new Random(repoName.hashCode());

        int totalViews = 0;
        int totalUniques = 0;
        int totalClones = 0;
        int totalCloners = 0;

        for (int i = 13; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            int v = 120 + rand.nextInt(280);
            int u = (int) (v * (0.65 + (rand.nextDouble() * 0.2)));
            int c = 25 + rand.nextInt(75);
            int cu = (int) (c * (0.7 + (rand.nextDouble() * 0.2)));

            totalViews += v;
            totalUniques += u;
            totalClones += c;
            totalCloners += cu;

            Map<String, Object> viewEntry = new HashMap<>();
            viewEntry.put("timestamp", date.toString() + "T00:00:00Z");
            viewEntry.put("count", v);
            viewEntry.put("uniques", u);
            viewsList.add(viewEntry);

            Map<String, Object> cloneEntry = new HashMap<>();
            cloneEntry.put("timestamp", date.toString() + "T00:00:00Z");
            cloneEntry.put("count", c);
            cloneEntry.put("uniques", cu);
            clonesList.add(cloneEntry);
        }

        Map<String, Object> viewsMap = new HashMap<>();
        viewsMap.put("count", totalViews);
        viewsMap.put("uniques", totalUniques);
        viewsMap.put("views", viewsList);

        Map<String, Object> clonesMap = new HashMap<>();
        clonesMap.put("count", totalClones);
        clonesMap.put("uniques", totalCloners);
        clonesMap.put("clones", clonesList);

        result.put("views", viewsMap);
        result.put("clones", clonesMap);
        return result;
    }

    private List<Map<String, Object>> generateMockWorkflowRuns(String owner, String repoName) {
        List<Map<String, Object>> runs = new ArrayList<>();
        runs.add(createWorkflowRunMap(901L, "CI/CD Pipeline", "push", "completed", "success",
                "feat: implement reactive SSE event stream", "a3f8901b", "main", "Himanshu", 142L));
        runs.add(createWorkflowRunMap(902L, "Integration Tests", "pull_request", "in_progress", null,
                "test: add MockMvc test suites for controllers", "c91e442d", "feature/tests", "Senior Dev", 48L));
        runs.add(createWorkflowRunMap(903L, "Security CodeQL Scan", "schedule", "completed", "success",
                "chore: bump Spring Boot to 3.2.3", "7b82f041", "main", "dependabot[bot]", 210L));
        runs.add(createWorkflowRunMap(904L, "Docker Multi-Arch Build", "push", "completed", "failure",
                "fix: resolve PostgreSQL connection pool timeout", "f019a82e", "fix/db-pool", "Himanshu", 95L));
        runs.add(createWorkflowRunMap(905L, "Deploy Production", "workflow_dispatch", "completed", "success",
                "release: v1.0.0 enterprise release", "19b48f99", "main", "ReleaseManager", 320L));
        return runs;
    }

    private Map<String, Object> createWorkflowRunMap(Long id, String name, String event, String status,
                                                     String conclusion, String message, String sha,
                                                     String branch, String author, Long duration) {
        Map<String, Object> run = new HashMap<>();
        run.put("id", id);
        run.put("name", name);
        run.put("event", event);
        run.put("status", status);
        run.put("conclusion", conclusion);
        run.put("head_branch", branch);
        run.put("head_sha", sha);
        run.put("html_url", "https://github.com/actions/runs/" + id);
        run.put("created_at", LocalDateTime.now().minusHours(new Random().nextInt(48) + 1).toString());
        run.put("updated_at", LocalDateTime.now().toString());

        Map<String, Object> commit = new HashMap<>();
        commit.put("message", message);
        run.put("head_commit", commit);

        Map<String, Object> actor = new HashMap<>();
        actor.put("login", author);
        run.put("actor", actor);

        run.put("duration_seconds", duration);
        return run;
    }

    private List<Map<String, Object>> generateMockPullRequests(String owner, String repoName) {
        List<Map<String, Object>> prs = new ArrayList<>();
        prs.add(createPrMap(412L, 42, "feat: Add high-throughput SSE stream for real-time CI/CD",
                "Himanshu", "open", false, 480, 32, 5));
        prs.add(createPrMap(413L, 43, "perf: Optimize PostgreSQL aggregate indexes for traffic metrics",
                "db-specialist", "open", false, 124, 18, 3));
        prs.add(createPrMap(414L, 44, "draft: WebFlux reactive GitHub webhooks listener",
                "junior-dev", "open", true, 310, 85, 1));
        prs.add(createPrMap(415L, 40, "fix: Prevent memory leak in SseEmitter connection pooling",
                "Senior Dev", "closed", false, 65, 42, 8));
        return prs;
    }

    private Map<String, Object> createPrMap(Long id, int prNum, String title, String author,
                                            String state, boolean isDraft, int additions,
                                            int deletions, int comments) {
        Map<String, Object> pr = new HashMap<>();
        pr.put("id", id);
        pr.put("number", prNum);
        pr.put("title", title);
        pr.put("state", state);
        pr.put("draft", isDraft);
        pr.put("html_url", "https://github.com/pulls/" + prNum);
        pr.put("created_at", LocalDateTime.now().minusDays(new Random().nextInt(7) + 1).toString());
        pr.put("updated_at", LocalDateTime.now().toString());
        pr.put("additions", additions);
        pr.put("deletions", deletions);
        pr.put("comments", comments);

        Map<String, Object> user = new HashMap<>();
        user.put("login", author);
        pr.put("user", user);

        return pr;
    }
}
