package com.pm.authservice.dto;

public record ClaimsResponse(String userId, String email, String role) {
}
