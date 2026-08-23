package com.gitpulse.backend.exception;

/**
 * Exception thrown when a requested database entity (repository, workflow, PR) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
