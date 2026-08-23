package com.gitpulse.backend.exception;

/**
 * Exception thrown when upstream GitHub API calls fail, hit rate limits, or return unauthorized.
 */
public class GitHubApiException extends RuntimeException {
    public GitHubApiException(String message) {
        super(message);
    }

    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
