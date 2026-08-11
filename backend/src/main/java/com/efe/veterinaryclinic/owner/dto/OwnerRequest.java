package com.efe.veterinaryclinic.owner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OwnerRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must be at most 100 characters")
        String firstName,

        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must be at most 100 characters")
        String lastName,

        @NotBlank(message = "must not be blank")
        @Size(max = 20, message = "must be at most 20 characters")
        String phone,

        @NotBlank(message = "must not be blank")
        @Size(max = 254, message = "must be at most 254 characters")
        @Email(message = "must be a valid email address")
        String email,

        @NotBlank(message = "must not be blank")
        @Size(max = 500, message = "must be at most 500 characters")
        String address
) {
}
