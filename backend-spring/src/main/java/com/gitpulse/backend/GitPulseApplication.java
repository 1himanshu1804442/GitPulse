package com.gitpulse.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the GitPulse Spring Boot 3 enterprise application.
 *
 * Why @EnableScheduling:
 * Enables background periodic polling and automated metrics synchronization
 * with GitHub API and SSE heartbeat transmissions.
 */
@SpringBootApplication
@EnableScheduling
public class GitPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitPulseApplication.class, args);
    }
}
