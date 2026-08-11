package com.efe.veterinaryclinic.pet;

import com.efe.veterinaryclinic.common.dto.PageResponse;
import com.efe.veterinaryclinic.common.exception.BusinessRuleViolationException;
import com.efe.veterinaryclinic.common.exception.ResourceNotFoundException;
import com.efe.veterinaryclinic.owner.Owner;
import com.efe.veterinaryclinic.owner.OwnerRepository;
import com.efe.veterinaryclinic.pet.dto.PetRequest;
import com.efe.veterinaryclinic.pet.dto.PetResponse;
import com.efe.veterinaryclinic.pet.dto.PetStatsResponse;
import com.efe.veterinaryclinic.visit.Visit;
import com.efe.veterinaryclinic.visit.VisitRepository;
import com.efe.veterinaryclinic.visit.VisitStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class  PetService {

    private static final Set<String> SPECIES_WITH_BREED = Set.of("CAT", "DOG");
    private static final int INACTIVE_YEARS_THRESHOLD = 2;

    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;
    private final VisitRepository visitRepository;

    public PetService(PetRepository petRepository, OwnerRepository ownerRepository, VisitRepository visitRepository) {
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
        this.visitRepository = visitRepository;
    }

    public PetResponse create(PetRequest request) {
        validateSpeciesBreedRule(request.species(), request.speciesNote());
        Owner owner = findOwnerOrThrow(request.ownerId());
        reactivateOwnerIfArchived(owner);

        Pet pet = new Pet(owner, request.name(), request.species(), request.breed(), request.speciesNote(),
                request.birthDate(), request.sex(), request.weightKg(), request.allergies(), request.chronicConditions());
        Pet saved = petRepository.save(pet);

        return PetResponse.from(saved, isInactive(saved));
    }

    public PageResponse<PetResponse> list(String search, String species, Long ownerId, Boolean active, Pageable pageable) {
        Specification<Pet> spec = (root, query, cb) -> cb.conjunction();

        if (search != null && !search.isBlank()) {
            spec = spec.and(PetSpecifications.nameContains(search));
        }
        if (species != null && !species.isBlank()) {
            spec = spec.and(PetSpecifications.hasSpecies(species));
        }
        if (ownerId != null) {
            spec = spec.and(PetSpecifications.hasOwnerId(ownerId));
        }
        if (active != null) {
            spec = spec.and(PetSpecifications.isArchived(!active));
        }

        return PageResponse.from(petRepository.findAll(spec, pageable).map(pet -> PetResponse.from(pet, isInactive(pet))));
    }

    public List<Pet> searchTop(String search, int limit) {
        return petRepository.findAll(PetSpecifications.nameContains(search), PageRequest.of(0, limit)).getContent();
    }

    public PetResponse getById(Long id) {
        Pet pet = findPetOrThrow(id);
        return PetResponse.from(pet, isInactive(pet));
    }

    public PetResponse update(Long id, PetRequest request) {
        validateSpeciesBreedRule(request.species(), request.speciesNote());
        Pet pet = findPetOrThrow(id);
        Owner owner = findOwnerOrThrow(request.ownerId());
        reactivateOwnerIfArchived(owner);

        pet.update(owner, request.name(), request.species(), request.breed(), request.speciesNote(),
                request.birthDate(), request.sex(), request.weightKg(), request.allergies(), request.chronicConditions());
        Pet saved = petRepository.save(pet);

        return PetResponse.from(saved, isInactive(saved));
    }

    public PetResponse archive(Long id) {
        Pet pet = findPetOrThrow(id);
        pet.archive();
        Pet saved = petRepository.save(pet);

        return PetResponse.from(saved, isInactive(saved));
    }

    public PetResponse activate(Long id) {
        Pet pet = findPetOrThrow(id);
        pet.activate();
        Pet saved = petRepository.save(pet);
        reactivateOwnerIfArchived(saved.getOwner());

        return PetResponse.from(saved, isInactive(saved));
    }

    public PetStatsResponse getStats() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);

        long totalPets = petRepository.countByArchivedFalse();
        long dogs = petRepository.countByArchivedFalseAndSpeciesIgnoreCase("DOG");
        long cats = petRepository.countByArchivedFalseAndSpeciesIgnoreCase("CAT");
        long newThisMonth = petRepository.countByCreatedAtGreaterThanEqual(startOfMonth.atStartOfDay());

        return new PetStatsResponse(totalPets, dogs, cats, newThisMonth);
    }

    public boolean isInactive(Pet pet) {
        LocalDateTime lastSeenAt = visitRepository
                .findTopByPet_IdAndStatusNotOrderByScheduledAtDesc(pet.getId(), VisitStatus.CANCELLED)
                .map(Visit::getScheduledAt)
                .orElse(pet.getCreatedAt());

        return lastSeenAt.isBefore(LocalDateTime.now().minusYears(INACTIVE_YEARS_THRESHOLD));
    }

    private void reactivateOwnerIfArchived(Owner owner) {
        if (owner.isArchived()) {
            owner.activate();
            ownerRepository.save(owner);
        }
    }

    private void validateSpeciesBreedRule(String species, String speciesNote) {
        boolean isCatOrDog = SPECIES_WITH_BREED.contains(species.toUpperCase());
        if (!isCatOrDog && (speciesNote == null || speciesNote.isBlank())) {
            throw new BusinessRuleViolationException("speciesNote",
                    "speciesNote is required when species is not CAT or DOG");
        }
    }

    private Owner findOwnerOrThrow(Long ownerId) {
        return ownerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found with id " + ownerId));
    }

    private Pet findPetOrThrow(Long id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with id " + id));
    }
}
