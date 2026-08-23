package com.gitpulse.backend.repository;

import com.gitpulse.backend.model.entity.RepositorySummary;
import com.gitpulse.backend.model.entity.TrafficMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Traffic Metrics.
 */
@Repository
public interface TrafficMetricRepository extends JpaRepository<TrafficMetric, Long> {
    List<TrafficMetric> findByRepositoryOrderByDateAsc(RepositorySummary repository);
    List<TrafficMetric> findByRepositoryAndDateBetweenOrderByDateAsc(RepositorySummary repository, LocalDate startDate, LocalDate endDate);
    Optional<TrafficMetric> findByRepositoryAndDate(RepositorySummary repository, LocalDate date);
}
