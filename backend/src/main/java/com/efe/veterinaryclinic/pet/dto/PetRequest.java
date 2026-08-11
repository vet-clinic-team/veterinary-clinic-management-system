package com.efe.veterinaryclinic.pet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PetRequest(
        @NotNull(message = "must not be null")
        Long ownerId,

        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must be at most 100 characters")
        String name,

        @NotBlank(message = "must not be blank")
        @Size(max = 50, message = "must be at most 50 characters")
        String species,

        @Size(max = 100, message = "must be at most 100 characters")
        String breed,

        @Size(max = 200, message = "must be at most 200 characters")
        String speciesNote,

        @NotNull(message = "must not be null")
        LocalDate birthDate,

        @NotBlank(message = "must not be blank")
        @Size(max = 20, message = "must be at most 20 characters")
        String sex,

        @NotNull(message = "must not be null")
        @Positive(message = "must be positive")
        Double weightKg,

        @Size(max = 500, message = "must be at most 500 characters")
        String allergies,

        @Size(max = 500, message = "must be at most 500 characters")
        String chronicConditions
) {
}
