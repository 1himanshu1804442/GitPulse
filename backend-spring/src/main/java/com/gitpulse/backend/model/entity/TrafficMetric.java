package com.gitpulse.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entity representing daily traffic analytics (views, clones, unique visitors) for a repository.
 *
 * Why JPA Entity:
 * Stores historical daily metrics so GitPulse can render 14-day trend charts, velocity tracking,
 * and growth forecasting.
 */
@Entity
@Table(name = "traffic_metrics", uniqueConstraints = {
        @UniqueConstraint(name = "uk_repo_date", columnNames = {"repository_id", "metric_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private RepositorySummary repository;

    @Column(name = "metric_date", nullable = false)
    private LocalDate date;

    @Column(name = "views_count")
    @Builder.Default
    private Integer viewsCount = 0;

    @Column(name = "unique_visitors")
    @Builder.Default
    private Integer uniqueVisitors = 0;

    @Column(name = "clones_count")
    @Builder.Default
    private Integer clonesCount = 0;

    @Column(name = "unique_cloners")
    @Builder.Default
    private Integer uniqueCloners = 0;
}
