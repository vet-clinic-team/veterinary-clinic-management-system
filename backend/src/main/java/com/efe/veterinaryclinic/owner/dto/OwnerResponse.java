package com.efe.veterinaryclinic.owner.dto;

import com.efe.veterinaryclinic.owner.Owner;

import java.time.LocalDateTime;

public record OwnerResponse(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String email,
        String address,
        long petCount,
        boolean archived,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static OwnerResponse from(Owner owner, long petCount) {
        return new OwnerResponse(
                owner.getId(),
                owner.getFirstName(),
                owner.getLastName(),
                owner.getPhone(),
                owner.getEmail(),
                owner.getAddress(),
                petCount,
                owner.isArchived(),
                owner.getCreatedAt(),
                owner.getUpdatedAt()
        );
    }
}
