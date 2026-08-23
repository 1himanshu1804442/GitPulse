package com.gitpulse.backend;

import com.gitpulse.backend.model.dto.AuthResponseDTO;
import com.gitpulse.backend.model.dto.ReRunResponseDTO;
import com.gitpulse.backend.model.entity.GitHubAccount;
import com.gitpulse.backend.model.entity.RepositorySummary;
import com.gitpulse.backend.model.entity.WorkflowRun;
import com.gitpulse.backend.repository.*;
import com.gitpulse.backend.service.GitHubClientService;
import com.gitpulse.backend.service.GitHubSyncService;
import com.gitpulse.backend.service.RealTimeEventsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration & Service test for GitHubSyncService against H2 in-memory database.
 *
 * Why @SpringBootTest with H2:
 * Tests the complete Spring Data JPA persistence lifecycle, ensuring entities,
 * unique constraints, relations, and transactions execute flawlessly.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GitHubSyncServiceTest {

    @Autowired
    private GitHubSyncService gitHubSyncService;

    @Autowired
    private GitHubAccountRepository accountRepository;

    @Autowired
    private RepositorySummaryRepository repoRepository;

    @Autowired
    private WorkflowRunRepository workflowRepository;

    @Autowired
    private TrafficMetricRepository trafficRepository;

    @Autowired
    private PullRequestSummaryRepository prRepository;

    @MockBean
    private GitHubClientService gitHubClientService;

    @MockBean
    private RealTimeEventsService realTimeEventsService;

    @BeforeEach
    void setupMocks() {
        when(gitHubClientService.fetchUserInfo(anyString())).thenReturn(Map.of(
                "login", "test-architect",
                "avatar_url", "https://avatars.githubusercontent.com/u/12345"
        ));

        Map<String, Object> mockRepo = new HashMap<>();
        mockRepo.put("id", 501L);
        mockRepo.put("name", "gitpulse-engine");
        mockRepo.put("full_name", "test-architect/gitpulse-engine");
        mockRepo.put("owner", Map.of("login", "test-architect"));
        mockRepo.put("description", "Real-time engine");
        mockRepo.put("stargazers_count", 450);
        mockRepo.put("forks_count", 50);
        mockRepo.put("open_issues_count", 3);
        mockRepo.put("watchers_count", 450);
        mockRepo.put("language", "Java");
        mockRepo.put("default_branch", "main");
        mockRepo.put("private", false);

        when(gitHubClientService.fetchUserRepositories(any(), any())).thenReturn(List.of(mockRepo));

        when(gitHubClientService.fetchRepositoryTraffic(any(), any(), any())).thenReturn(Map.of(
                "views", Map.of("count", 100, "uniques", 80, "views", List.of()),
                "clones", Map.of("count", 20, "uniques", 15, "clones", List.of())
        ));

        Map<String, Object> mockRun = new HashMap<>();
        mockRun.put("id", 999L);
        mockRun.put("name", "CI Build");
        mockRun.put("event", "push");
        mockRun.put("status", "completed");
        mockRun.put("conclusion", "success");
        mockRun.put("head_branch", "main");
        mockRun.put("head_sha", "c7a8b9f0");
        mockRun.put("duration_seconds", 120L);

        when(gitHubClientService.fetchWorkflowRuns(any(), any(), any())).thenReturn(List.of(mockRun));

        Map<String, Object> mockPr = new HashMap<>();
        mockPr.put("id", 777L);
        mockPr.put("number", 1);
        mockPr.put("title", "Initial Architecture Setup");
        mockPr.put("state", "open");
        mockPr.put("draft", false);
        mockPr.put("additions", 200);
        mockPr.put("deletions", 10);
        mockPr.put("comments", 2);

        when(gitHubClientService.fetchPullRequests(any(), any(), any())).thenReturn(List.of(mockPr));

        when(gitHubClientService.triggerWorkflowReRun(any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("connectAccount persists GitHubAccount and triggers repository sync")
    void testConnectAccount() {
        AuthResponseDTO response = gitHubSyncService.connectAccount("ghp_testToken123", "test-architect");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getUsername()).isEqualTo("test-architect");

        Optional<GitHubAccount> account = accountRepository.findByUsername("test-architect");
        assertThat(account).isPresent();
        assertThat(account.get().getAvatarUrl()).isEqualTo("https://avatars.githubusercontent.com/u/12345");

        List<RepositorySummary> repos = repoRepository.findAll();
        assertThat(repos).isNotEmpty();
        assertThat(repos.get(0).getName()).isEqualTo("gitpulse-engine");
        assertThat(repos.get(0).getStarsCount()).isEqualTo(450);
    }

    @Test
    @DisplayName("triggerWorkflowRerun updates status to in_progress")
    void testTriggerWorkflowRerun() {
        // First sync to seed workflow
        gitHubSyncService.syncAllData("token", "test-architect");

        Optional<WorkflowRun> runOpt = workflowRepository.findByGithubRunId(999L);
        assertThat(runOpt).isPresent();

        ReRunResponseDTO rerunResp = gitHubSyncService.triggerWorkflowRerun(runOpt.get().getId());
        assertThat(rerunResp.isSuccess()).isTrue();
        assertThat(rerunResp.getStatus()).isEqualTo("in_progress");

        WorkflowRun updatedRun = workflowRepository.findById(runOpt.get().getId()).orElseThrow();
        assertThat(updatedRun.getStatus()).isEqualTo("in_progress");
        assertThat(updatedRun.getConclusion()).isNull();
    }
}
