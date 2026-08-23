package com.gitpulse.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for connecting a GitHub account with a Personal Access Token (PAT).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenAuthRequestDTO {

    @NotBlank(message = "GitHub Personal Access Token (PAT) is required")
    private String token;

    private String username;
}
