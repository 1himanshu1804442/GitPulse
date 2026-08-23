package com.gitpulse.backend;

import com.gitpulse.backend.controller.DashboardController;
import com.gitpulse.backend.model.dto.DashboardSummaryDTO;
import com.gitpulse.backend.model.dto.RepositoryDTO;
import com.gitpulse.backend.model.dto.TrafficAnalyticsDTO;
import com.gitpulse.backend.service.TrafficAnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit test suite for DashboardController using MockMvc.
 *
 * Why @WebMvcTest:
 * Focuses strictly on the Spring MVC layer (routing, HTTP serialization, status codes)
 * without bootstrapping the entire database context, providing lightning-fast unit test execution.
 */
@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrafficAnalyticsService trafficAnalyticsService;

    @Test
    @DisplayName("GET /api/v1/dashboard/summary - Returns 200 OK with aggregated statistics")
    void testGetDashboardSummary() throws Exception {
        DashboardSummaryDTO mockSummary = DashboardSummaryDTO.builder()
                .totalRepositories(5)
                .totalStars(8500)
                .totalForks(920)
                .totalOpenIssues(32)
                .totalViews14d(12450)
                .totalClones14d(1830)
                .totalWorkflowsRun(54)
                .activeWorkflowsCount(2)
                .pendingReviewPrsCount(4)
                .topLanguages(Map.of("Java", 3, "TypeScript", 2))
                .build();

        when(trafficAnalyticsService.getOverallDashboardSummary()).thenReturn(mockSummary);

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRepositories").value(5))
                .andExpect(jsonPath("$.totalStars").value(8500))
                .andExpect(jsonPath("$.totalForks").value(920))
                .andExpect(jsonPath("$.totalViews14d").value(12450))
                .andExpect(jsonPath("$.topLanguages.Java").value(3));
    }

    @Test
    @DisplayName("GET /api/v1/repos - Returns 200 OK with repository list")
    void testGetAllRepositories() throws Exception {
        RepositoryDTO repo1 = RepositoryDTO.builder()
                .id(1L)
                .githubRepoId(101L)
                .name("gitpulse-core")
                .fullName("octocat/gitpulse-core")
                .owner("octocat")
                .starsCount(1240)
                .forksCount(185)
                .language("Java")
                .isPrivate(false)
                .lastSyncedAt(LocalDateTime.now())
                .build();

        when(trafficAnalyticsService.getAllRepositories()).thenReturn(List.of(repo1));

        mockMvc.perform(get("/api/v1/repos")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("gitpulse-core"))
                .andExpect(jsonPath("$[0].fullName").value("octocat/gitpulse-core"))
                .andExpect(jsonPath("$[0].starsCount").value(1240))
                .andExpect(jsonPath("$[0].language").value("Java"));
    }

    @Test
    @DisplayName("GET /api/v1/traffic - Returns 200 OK with traffic metrics")
    void testGetTrafficAnalytics() throws Exception {
        TrafficAnalyticsDTO trafficDTO = TrafficAnalyticsDTO.builder()
                .repoFullName("octocat/gitpulse-core")
                .totalViews(3500)
                .totalUniqueVisitors(2100)
                .totalClones(450)
                .totalUniqueCloners(320)
                .velocityScore(250.0)
                .dailyPoints(List.of(
                        TrafficAnalyticsDTO.DailyTrafficPointDTO.builder()
                                .date(LocalDate.now())
                                .views(250)
                                .uniqueVisitors(180)
                                .clones(35)
                                .uniqueCloners(28)
                                .build()
                ))
                .build();

        when(trafficAnalyticsService.getRepositoryTraffic(any())).thenReturn(trafficDTO);

        mockMvc.perform(get("/api/v1/traffic")
                        .param("repo", "octocat/gitpulse-core")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repoFullName").value("octocat/gitpulse-core"))
                .andExpect(jsonPath("$.totalViews").value(3500))
                .andExpect(jsonPath("$.dailyPoints[0].views").value(250));
    }
}
