package com.gitpulse.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Payload broadcast over Server-Sent Events (SSE) to update the React client in real time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveEventDTO {
    private String eventType; // e.g. "WORKFLOW_STATUS", "SYNC_COMPLETED", "PR_UPDATED", "METRIC_UPDATE"
    private String message;
    private Object payload;
    private LocalDateTime timestamp;
}
