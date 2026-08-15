package com.efe.veterinaryclinic.vaccination;

import com.efe.veterinaryclinic.common.dto.PageResponse;
import com.efe.veterinaryclinic.common.exception.ResourceNotFoundException;
import com.efe.veterinaryclinic.pet.Pet;
import com.efe.veterinaryclinic.pet.PetRepository;
import com.efe.veterinaryclinic.security.Role;
import com.efe.veterinaryclinic.vaccination.dto.VaccinationRequest;
import com.efe.veterinaryclinic.vaccination.dto.VaccinationResponse;
import com.efe.veterinaryclinic.vaccination.dto.VaccinationStatsResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Locale;
import java.util.Set;

@Service
public class VaccinationService {

    // PDF only defines +1 year / +3 years based on vaccine type; it does not specify real vaccine names.
    // ONE_YEAR / THREE_YEAR are demo/example type identifiers until product defines the real vaccine catalog.
    private static final Period DEFAULT_INTERVAL = Period.ofYears(1);
    private static final Period THREE_YEAR_INTERVAL = Period.ofYears(3);
    private static final Set<String> THREE_YEAR_VACCINE_TYPES = Set.of("THREE_YEAR");

    private final VaccinationRepository vaccinationRepository;
    private final PetRepository petRepository;

    public VaccinationService(VaccinationRepository vaccinationRepository, PetRepository petRepository) {
        this.vaccinationRepository = vaccinationRepository;
        this.petRepository = petRepository;
    }

    public VaccinationResponse create(VaccinationRequest request, Role requesterRole) {
        if (requesterRole == Role.RECEPTIONIST) {
            throw new AccessDeniedException("RECEPTIONIST cannot create vaccination records");
        }

        Pet pet = findPetOrThrow(request.petId());
        LocalDate nextDueDate = calculateNextDueDate(request.vaccineType(), request.administeredAt());

        Vaccination vaccination = new Vaccination(pet, request.vaccineType(), request.administeredAt(),
                request.lotNumber(), request.administeredBy(), nextDueDate);
        Vaccination saved = vaccinationRepository.save(vaccination);

        return VaccinationResponse.from(saved);
    }

    public VaccinationResponse getById(Long id) {
        return VaccinationResponse.from(findVaccinationOrThrow(id));
    }

    public PageResponse<VaccinationResponse> list(Pageable pageable) {
        return PageResponse.from(vaccinationRepository.findAll(pageable).map(VaccinationResponse::from));
    }

    public PageResponse<VaccinationResponse> listByPet(Long petId, Pageable pageable) {
        findPetOrThrow(petId);

        return PageResponse.from(vaccinationRepository.findByPet_Id(petId, pageable).map(VaccinationResponse::from));
    }

    public VaccinationResponse update(Long id, VaccinationRequest request, Role requesterRole) {
        if (requesterRole == Role.RECEPTIONIST) {
            throw new AccessDeniedException("RECEPTIONIST cannot update vaccination records");
        }

        Vaccination vaccination = findVaccinationOrThrow(id);
        Pet pet = findPetOrThrow(request.petId());
        LocalDate nextDueDate = calculateNextDueDate(request.vaccineType(), request.administeredAt());

        vaccination.update(pet, request.vaccineType(), request.administeredAt(), request.lotNumber(),
                request.administeredBy(), nextDueDate);

        return VaccinationResponse.from(vaccinationRepository.save(vaccination));
    }

    public void delete(Long id, Role requesterRole) {
        if (requesterRole == Role.RECEPTIONIST) {
            throw new AccessDeniedException("RECEPTIONIST cannot delete vaccination records");
        }

        vaccinationRepository.delete(findVaccinationOrThrow(id));
    }

    public VaccinationStatsResponse getStats() {
        LocalDate today = LocalDate.now();

        long totalVaccinations = vaccinationRepository.count();
        long administeredToday = vaccinationRepository.countByAdministeredAtBetween(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        long upcomingDue = vaccinationRepository.countByNextDueDateAfter(today);
        long vaccinatedPets = vaccinationRepository.countDistinctPet();

        return new VaccinationStatsResponse(totalVaccinations, administeredToday, upcomingDue, vaccinatedPets);
    }

    private LocalDate calculateNextDueDate(String vaccineType, LocalDateTime administeredAt) {
        Period interval = THREE_YEAR_VACCINE_TYPES.contains(vaccineType.trim().toUpperCase(Locale.ROOT))
                ? THREE_YEAR_INTERVAL
                : DEFAULT_INTERVAL;

        return administeredAt.toLocalDate().plus(interval);
    }

    private Vaccination findVaccinationOrThrow(Long id) {
        return vaccinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccination not found with id " + id));
    }

    private Pet findPetOrThrow(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with id " + petId));
    }
}
