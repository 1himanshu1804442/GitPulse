package com.gitpulse.backend;

import com.gitpulse.backend.controller.WorkflowController;
import com.gitpulse.backend.model.dto.ReRunResponseDTO;
import com.gitpulse.backend.model.dto.WorkflowRunDTO;
import com.gitpulse.backend.service.GitHubSyncService;
import com.gitpulse.backend.service.TrafficAnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit test suite for WorkflowController using MockMvc.
 */
@WebMvcTest(WorkflowController.class)
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrafficAnalyticsService trafficAnalyticsService;

    @MockBean
    private GitHubSyncService gitHubSyncService;

    @Test
    @DisplayName("GET /api/v1/workflows - Returns 200 OK with workflow runs")
    void testGetWorkflowRuns() throws Exception {
        WorkflowRunDTO run = WorkflowRunDTO.builder()
                .id(1L)
                .githubRunId(901L)
                .repoFullName("octocat/gitpulse-core")
                .workflowName("CI/CD Pipeline")
                .event("push")
                .status("completed")
                .conclusion("success")
                .commitMessage("feat: add SSE real-time stream")
                .commitSha("a3f8901b")
                .branch("main")
                .author("Himanshu")
                .durationSeconds(142L)
                .htmlUrl("https://github.com/actions/runs/901")
                .createdAt(LocalDateTime.now())
                .build();

        when(trafficAnalyticsService.getWorkflowRuns(null)).thenReturn(List.of(run));

        mockMvc.perform(get("/api/v1/workflows")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].workflowName").value("CI/CD Pipeline"))
                .andExpect(jsonPath("$[0].status").value("completed"))
                .andExpect(jsonPath("$[0].conclusion").value("success"))
                .andExpect(jsonPath("$[0].commitSha").value("a3f8901b"));
    }

    @Test
    @DisplayName("POST /api/v1/workflows/rerun/{id} - Returns 200 OK with rerun response")
    void testRerunWorkflow() throws Exception {
        ReRunResponseDTO responseDTO = ReRunResponseDTO.builder()
                .success(true)
                .runId(901L)
                .status("in_progress")
                .message("Rerun dispatched successfully for CI/CD Pipeline")
                .build();

        when(gitHubSyncService.triggerWorkflowRerun(eq(901L))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/workflows/rerun/901")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.runId").value(901))
                .andExpect(jsonPath("$.status").value("in_progress"))
                .andExpect(jsonPath("$.message").value("Rerun dispatched successfully for CI/CD Pipeline"));
    }
}
