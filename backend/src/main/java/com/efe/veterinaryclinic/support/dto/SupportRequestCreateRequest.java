package com.efe.veterinaryclinic.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportRequestCreateRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 255, message = "must be at most 255 characters")
        String subject,

        @NotBlank(message = "must not be blank")
        @Size(max = 4000, message = "must be at most 4000 characters")
        String message
) {
}
