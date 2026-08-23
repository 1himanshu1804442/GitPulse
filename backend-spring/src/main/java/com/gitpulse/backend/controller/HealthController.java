package com.gitpulse.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health and diagnostics endpoint for container health probes and uptime monitoring.
 */
@Slf4j
@RestController
@RequestMapping({"/api/v1/health", "/api/health"})
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final Environment environment;

    /**
     * GET /api/v1/health
     * Returns operational status of the service and database connectivity.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("activeProfiles", environment.getActiveProfiles());

        boolean dbHealthy = false;
        try (Connection connection = dataSource.getConnection()) {
            dbHealthy = connection.isValid(2);
        } catch (Exception ex) {
            log.error("Database health check probe failed: {}", ex.getMessage(), ex);
        }

        health.put("database", dbHealthy ? "CONNECTED" : "DISCONNECTED");
        return ResponseEntity.ok(health);
    }
}
