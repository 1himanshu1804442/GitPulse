package com.gitpulse.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Traffic and Visitor Analytics.
 *
 * Why structured daily lists:
 * Allows frontend chart libraries (e.g. Recharts or Chart.js) to effortlessly plot
 * time-series line and bar charts of repository views and clones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficAnalyticsDTO {

    private String repoFullName;
    private Integer totalViews;
    private Integer totalUniqueVisitors;
    private Integer totalClones;
    private Integer totalUniqueCloners;
    private Double velocityScore;

    @Builder.Default
    private List<DailyTrafficPointDTO> dailyPoints = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTrafficPointDTO {
        private LocalDate date;
        private Integer views;
        private Integer uniqueVisitors;
        private Integer clones;
        private Integer uniqueCloners;
    }
}
