package com.gitpulse.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload returned when requesting a CI/CD workflow rerun.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReRunResponseDTO {
    private boolean success;
    private Long runId;
    private String message;
    private String status;
}
