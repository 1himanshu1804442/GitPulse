package com.gitpulse.backend.controller;

import com.gitpulse.backend.model.dto.AuthResponseDTO;
import com.gitpulse.backend.model.dto.TokenAuthRequestDTO;
import com.gitpulse.backend.service.GitHubSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller handling GitHub authentication, personal access tokens, and CLI detection.
 */
@Slf4j
@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final GitHubSyncService gitHubSyncService;

    /**
     * POST /api/v1/auth/connect or /api/v1/auth/token
     * Connects a GitHub account using a Personal Access Token (PAT).
     */
    @PostMapping({"/connect", "/token"})
    public ResponseEntity<AuthResponseDTO> connectToken(@Valid @RequestBody TokenAuthRequestDTO request) {
        log.info("REST request received: POST /auth/connect for user: {}", request.getUsername());
        AuthResponseDTO response = gitHubSyncService.connectAccount(request.getToken(), request.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/auth/status
     * Returns current authentication state and connected user details.
     */
    @GetMapping("/status")
    public ResponseEntity<AuthResponseDTO> getAuthStatus() {
        log.info("REST request received: GET /auth/status");
        AuthResponseDTO status = gitHubSyncService.getAccountStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * GET/POST /api/v1/auth/cli-detect or /api/auth/detect-cli
     * Automatically attempts to detect and connect GitHub credentials configured on the developer machine.
     */
    @RequestMapping(value = {"/cli-detect", "/detect-cli"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<AuthResponseDTO> detectCliAuth() {
        log.info("REST request received: /auth/cli-detect (auto-discovery)");
        AuthResponseDTO result = gitHubSyncService.detectCliAuth();
        return ResponseEntity.ok(result);
    }
}
