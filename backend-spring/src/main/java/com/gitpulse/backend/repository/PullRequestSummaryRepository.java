package com.gitpulse.backend.repository;

import com.gitpulse.backend.model.entity.PullRequestSummary;
import com.gitpulse.backend.model.entity.RepositorySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Pull Request Summaries.
 */
@Repository
public interface PullRequestSummaryRepository extends JpaRepository<PullRequestSummary, Long> {
    List<PullRequestSummary> findByRepositoryOrderByCreatedAtDesc(RepositorySummary repository);
    List<PullRequestSummary> findByStateOrderByCreatedAtDesc(String state);
    Optional<PullRequestSummary> findByGithubPrId(Long githubPrId);
    long countByState(String state);
}
