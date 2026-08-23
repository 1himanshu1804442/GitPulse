package com.gitpulse.backend.repository;

import com.gitpulse.backend.model.entity.RepositorySummary;
import com.gitpulse.backend.model.entity.WorkflowRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for GitHub Actions Workflow Runs.
 */
@Repository
public interface WorkflowRunRepository extends JpaRepository<WorkflowRun, Long> {
    List<WorkflowRun> findByRepositoryOrderByCreatedAtDesc(RepositorySummary repository);
    List<WorkflowRun> findTop20ByOrderByCreatedAtDesc();
    Optional<WorkflowRun> findByGithubRunId(Long githubRunId);
    long countByStatus(String status);
    long countByConclusion(String conclusion);
}
