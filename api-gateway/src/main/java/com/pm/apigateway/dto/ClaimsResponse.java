package com.pm.apigateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClaimsResponse(String userId, String email, String role) {
}
