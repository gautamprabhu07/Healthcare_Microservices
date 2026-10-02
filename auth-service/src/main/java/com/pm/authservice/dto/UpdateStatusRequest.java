package com.pm.authservice.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
    @NotNull(message = "Enabled flag is required") Boolean enabled) {
}
