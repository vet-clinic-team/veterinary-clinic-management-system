package com.efe.veterinaryclinic.owner;

import com.efe.veterinaryclinic.common.dto.PageResponse;
import com.efe.veterinaryclinic.common.exception.ConflictException;
import com.efe.veterinaryclinic.common.exception.ResourceNotFoundException;
import com.efe.veterinaryclinic.invoice.InvoiceService;
import com.efe.veterinaryclinic.owner.dto.OwnerDetailResponse;
import com.efe.veterinaryclinic.owner.dto.OwnerRequest;
import com.efe.veterinaryclinic.owner.dto.OwnerResponse;
import com.efe.veterinaryclinic.owner.dto.OwnerStatsResponse;
import com.efe.veterinaryclinic.pet.PetRepository;
import com.efe.veterinaryclinic.pet.PetService;
import com.efe.veterinaryclinic.pet.dto.PetResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final PetService petService;
    private final InvoiceService invoiceService;

    public OwnerService(OwnerRepository ownerRepository, PetRepository petRepository, PetService petService,
                         InvoiceService invoiceService) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.petService = petService;
        this.invoiceService = invoiceService;
    }

    public OwnerResponse create(OwnerRequest request) {
        Owner owner = new Owner(request.firstName(), request.lastName(), request.phone(),
                request.email(), request.address());
        Owner saved = ownerRepository.save(owner);

        return toResponse(saved);
    }

    public PageResponse<OwnerResponse> list(String search, Boolean active, Pageable pageable) {
        Specification<Owner> spec = (root, query, cb) -> cb.conjunction();

        if (search != null && !search.isBlank()) {
            spec = spec.and(OwnerSpecifications.nameContains(search));
        }
        if (active != null) {
            spec = spec.and(OwnerSpecifications.isArchived(!active));
        }

        return PageResponse.from(ownerRepository.findAll(spec, pageable).map(this::toResponse));
    }

    public List<Owner> searchTop(String search, int limit) {
        return ownerRepository.findAll(OwnerSpecifications.nameContains(search), PageRequest.of(0, limit)).getContent();
    }

    public OwnerDetailResponse getById(Long id) {
        Owner owner = findOrThrow(id);
        List<PetResponse> pets = petRepository.findByOwnerId(id).stream()
                .map(pet -> PetResponse.from(pet, petService.isInactive(pet)))
                .toList();

        return OwnerDetailResponse.from(owner, pets, invoiceService.getByOwnerId(id));
    }

    public OwnerResponse update(Long id, OwnerRequest request) {
        Owner owner = findOrThrow(id);
        owner.update(request.firstName(), request.lastName(), request.phone(), request.email(), request.address());

        return toResponse(ownerRepository.save(owner));
    }

    public OwnerStatsResponse getStats() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);

        long totalOwners = ownerRepository.count();
        long totalPets = petRepository.count();
        long newOwnersThisMonth = ownerRepository.countByCreatedAtGreaterThanEqual(startOfMonth.atStartOfDay());

        return new OwnerStatsResponse(totalOwners, totalPets, newOwnersThisMonth);
    }

    public OwnerResponse archive(Long id) {
        Owner owner = findOrThrow(id);
        if (petRepository.existsByOwnerIdAndArchivedFalse(id)) {
            throw new ConflictException("Owner has active pet(s) and cannot be archived");
        }

        owner.archive();
        return toResponse(ownerRepository.save(owner));
    }

    public OwnerResponse activate(Long id) {
        Owner owner = findOrThrow(id);
        owner.activate();
        return toResponse(ownerRepository.save(owner));
    }

    public void delete(Long id) {
        Owner owner = findOrThrow(id);
        if (!owner.isArchived()) {
            throw new ConflictException("Owner must be archived before it can be deleted");
        }

        long petCount = petRepository.countByOwnerId(owner.getId());
        if (petCount > 0) {
            throw new ConflictException("Owner has " + petCount + " pet(s) and cannot be deleted");
        }

        ownerRepository.delete(owner);
    }

    private Owner findOrThrow(Long id) {
        return ownerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found with id " + id));
    }

    private OwnerResponse toResponse(Owner owner) {
        return OwnerResponse.from(owner, petRepository.countByOwnerId(owner.getId()));
    }
}
