package com.efe.veterinaryclinic.owner.dto;

import com.efe.veterinaryclinic.invoice.dto.InvoiceResponse;
import com.efe.veterinaryclinic.owner.Owner;
import com.efe.veterinaryclinic.pet.dto.PetResponse;

import java.time.LocalDateTime;
import java.util.List;

public record OwnerDetailResponse(
        Long id,
        String firstName,
        String lastName,
        String phone,
        String email,
        String address,
        long petCount,
        List<PetResponse> pets,
        List<InvoiceResponse> invoices,
        boolean archived,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static OwnerDetailResponse from(Owner owner, List<PetResponse> pets, List<InvoiceResponse> invoices) {
        return new OwnerDetailResponse(
                owner.getId(),
                owner.getFirstName(),
                owner.getLastName(),
                owner.getPhone(),
                owner.getEmail(),
                owner.getAddress(),
                pets.size(),
                pets,
                invoices,
                owner.isArchived(),
                owner.getCreatedAt(),
                owner.getUpdatedAt()
        );
    }
}
