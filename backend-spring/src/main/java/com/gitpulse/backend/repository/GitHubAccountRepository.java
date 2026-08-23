package com.gitpulse.backend.repository;

import com.gitpulse.backend.model.entity.GitHubAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for GitHub Accounts.
 *
 * Why JpaRepository:
 * Provides standard CRUD and pagination methods out-of-the-box without boilerplate SQL.
 */
@Repository
public interface GitHubAccountRepository extends JpaRepository<GitHubAccount, Long> {
    Optional<GitHubAccount> findByUsername(String username);
    Optional<GitHubAccount> findFirstByOrderByIdAsc();
}
