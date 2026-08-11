package com.efe.veterinaryclinic.support.dto;

import com.efe.veterinaryclinic.support.SupportRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SupportRequestStatusUpdateRequest(
        @NotNull(message = "must not be null")
        SupportRequestStatus status,

        @Size(max = 4000, message = "must be at most 4000 characters")
        String adminResponse
) {
}
