package com.gitpulse.backend.repository;

import com.gitpulse.backend.model.entity.RepositorySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Repository Summaries.
 */
@Repository
public interface RepositorySummaryRepository extends JpaRepository<RepositorySummary, Long> {
    Optional<RepositorySummary> findByFullName(String fullName);
    Optional<RepositorySummary> findByGithubRepoId(Long githubRepoId);
    List<RepositorySummary> findByOwnerOrderByStarsCountDesc(String owner);
    List<RepositorySummary> findAllByOrderByStarsCountDesc();
}
