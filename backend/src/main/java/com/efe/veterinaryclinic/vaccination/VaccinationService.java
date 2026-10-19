package com.efe.veterinaryclinic.vaccination;

import com.efe.veterinaryclinic.common.dto.PageResponse;
import com.efe.veterinaryclinic.common.exception.ResourceNotFoundException;
import com.efe.veterinaryclinic.followup.FollowUpRuleService;
import com.efe.veterinaryclinic.followup.FollowUpService;
import com.efe.veterinaryclinic.pet.Pet;
import com.efe.veterinaryclinic.pet.PetRepository;
import com.efe.veterinaryclinic.security.Role;
import com.efe.veterinaryclinic.vaccination.dto.VaccinationRequest;
import com.efe.veterinaryclinic.vaccination.dto.VaccinationResponse;
import com.efe.veterinaryclinic.vaccination.dto.VaccinationStatsResponse;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.efe.veterinaryclinic.catalog.ProcedureCatalogService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class VaccinationService {

    private final VaccinationRepository vaccinationRepository;
    private final PetRepository petRepository;
    private final FollowUpRuleService followUpRuleService;
    private final FollowUpService followUpService;
    private final ProcedureCatalogService catalogService;

    
public VaccinationService(
        VaccinationRepository vaccinationRepository,
        PetRepository petRepository,
        FollowUpRuleService followUpRuleService,
        FollowUpService followUpService,
        ProcedureCatalogService catalogService) {

    this.vaccinationRepository = vaccinationRepository;
    this.petRepository = petRepository;
    this.followUpRuleService = followUpRuleService;
    this.followUpService = followUpService;
    this.catalogService = catalogService;
}


    @Transactional
    public VaccinationResponse create(
            VaccinationRequest request,
            Role requesterRole) {

        checkWritePermission(requesterRole);

        Pet pet = findPetOrThrow(request.petId());
        
String species = "OTHER".equalsIgnoreCase(pet.getSpecies())
        ? pet.getSpeciesNote()
        : pet.getSpecies();

var procedure = catalogService.requireActive(
        request.vaccineType(),
        species
);

if (!"VACCINATION".equalsIgnoreCase(procedure.getCategory())) {
    throw new IllegalArgumentException(
            "Selected procedure is not a vaccination"
    );
}


        Vaccination vaccination = new Vaccination(
                pet,
                procedure.getCode(),
                request.administeredAt(),
                request.lotNumber(),
                request.administeredBy(),
                null
        );

        Vaccination saved =
                vaccinationRepository.saveAndFlush(vaccination);

        recalculateVaccinationSchedule(
                pet.getId(),
                procedure.getCode()
        );

        
followUpService.completePreviousVaccinationFollowUp(
        pet.getId(),
        procedure.getCode(),
        request.administeredAt()
);


        return VaccinationResponse.from(saved);
    }

    public VaccinationResponse getById(Long id) {

        return VaccinationResponse.from(
                findVaccinationOrThrow(id)
        );
    }

    public PageResponse<VaccinationResponse> list(
            Pageable pageable) {

        return PageResponse.from(
                vaccinationRepository.findAll(pageable)
                        .map(VaccinationResponse::from)
        );
    }

    public PageResponse<VaccinationResponse> listByPet(
            Long petId,
            Pageable pageable) {

        findPetOrThrow(petId);

        return PageResponse.from(
                vaccinationRepository.findByPet_Id(petId, pageable)
                        .map(VaccinationResponse::from)
        );
    }

    
@Transactional
public VaccinationResponse update(
        Long id,
        VaccinationRequest request,
        Role requesterRole) {

    checkWritePermission(requesterRole);

    Vaccination vaccination = findVaccinationOrThrow(id);

    Long oldPetId = vaccination.getPet().getId();
    String oldVaccineType = vaccination.getVaccineType();

    Pet newPet = findPetOrThrow(request.petId());

    String species = "OTHER".equalsIgnoreCase(newPet.getSpecies())
            ? newPet.getSpeciesNote()
            : newPet.getSpecies();

    String vaccineCode;

    boolean unchangedLegacyType =
            Objects.equals(oldPetId, newPet.getId())
            && oldVaccineType.equalsIgnoreCase(
                    request.vaccineType().trim()
            );

    if (unchangedLegacyType) {
        // Preserve existing records, including legacy vaccine types.
        vaccineCode = oldVaccineType;
    } else {
        var procedure = catalogService.requireActive(
                request.vaccineType(),
                species
        );

        if (!"VACCINATION".equalsIgnoreCase(procedure.getCategory())) {
            throw new IllegalArgumentException(
                    "Selected procedure is not a vaccination"
            );
        }

        vaccineCode = procedure.getCode();
    }

    vaccination.update(
            newPet,
            vaccineCode,
            request.administeredAt(),
            request.lotNumber(),
            request.administeredBy(),
            null
    );

    Vaccination saved =
            vaccinationRepository.saveAndFlush(vaccination);

    boolean groupChanged =
            !Objects.equals(oldPetId, newPet.getId())
                    || !oldVaccineType.equalsIgnoreCase(vaccineCode);

    if (groupChanged) {
        recalculateVaccinationSchedule(
                oldPetId,
                oldVaccineType
        );
    }

    recalculateVaccinationSchedule(
            newPet.getId(),
            vaccineCode
    );

    return VaccinationResponse.from(saved);
}


    @Transactional
    public void delete(
            Long id,
            Role requesterRole) {

        checkWritePermission(requesterRole);

        Vaccination vaccination = findVaccinationOrThrow(id);

        Long petId = vaccination.getPet().getId();
        String vaccineType = vaccination.getVaccineType();

        followUpService.cancelFromVaccination(
                vaccination.getId()
        );

        vaccinationRepository.delete(vaccination);
        vaccinationRepository.flush();

        recalculateVaccinationSchedule(
                petId,
                vaccineType
        );
    }
    

@Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
public void recalculateForNewRule(String species, String vaccineType) {


    List<Long> petIds =
            vaccinationRepository.findPetIdsByVaccineTypeAndSpecies(
                    vaccineType,
                    species
            );

    System.out.println("RECALCULATION PET IDS: " + petIds);

    for (Long petId : petIds) {
        recalculateVaccinationSchedule(petId, vaccineType);
    }
}


    private void recalculateVaccinationSchedule(
            Long petId,
            String vaccineType) {

        List<Vaccination> vaccinations = vaccinationRepository
                .findByPet_IdAndVaccineTypeIgnoreCaseOrderByAdministeredAtAscIdAsc(
                        petId,
                        vaccineType
                );

        for (Vaccination vaccination : vaccinations) {

            Pet pet = vaccination.getPet();

            LocalDate newDueDate = followUpRuleService
                    .calculateNextDueDateForUpdate(
                            vaccination.getId(),
                            pet.getId(),
                            "OTHER".equalsIgnoreCase(pet.getSpecies())
        ? (pet.getSpeciesNote() == null
                || pet.getSpeciesNote().isBlank()
                ? "OTHER"
                : pet.getSpeciesNote().trim())
        : pet.getSpecies(),
                            

                            vaccination.getVaccineType(),
                            vaccination.getAdministeredAt().toLocalDate()
                    )
                    .orElse(null);

            // Update the vaccination's calculated next due date.
            vaccination.updateNextDueDate(newDueDate);

            // Synchronize its associated follow-up record.
            followUpService.syncFromVaccination(
                    pet,
                    vaccination.getVaccineType(),
                    newDueDate,
                    vaccination.getId()
            );
        }

        // Re-evaluate follow-up completion after recalculation.
        followUpService.reconcileVaccinationFollowUps(
                petId,
                vaccineType
        );
    }

    public VaccinationStatsResponse getStats() {

        LocalDate today = LocalDate.now();

        LocalDateTime startOfToday =
                today.atStartOfDay();

        LocalDateTime startOfTomorrow =
                today.plusDays(1).atStartOfDay();

        long total = vaccinationRepository.count();

        long administeredToday = vaccinationRepository
                .countByAdministeredAtBetween(
                        startOfToday,
                        startOfTomorrow.minusNanos(1)
                );

        long upcoming = vaccinationRepository
                .countByNextDueDateAfter(today);

        long vaccinatedPets = vaccinationRepository
                .countDistinctPet();

        return new VaccinationStatsResponse(
                total,
                administeredToday,
                upcoming,
                vaccinatedPets
        );
    }

    private void checkWritePermission(
            Role requesterRole) {

        if (requesterRole == Role.RECEPTIONIST) {
            throw new AccessDeniedException(
                    "RECEPTIONIST cannot modify vaccination records"
            );
        }
    }

    private Vaccination findVaccinationOrThrow(Long id) {

        return vaccinationRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vaccination not found: " + id
                        )
                );
    }

    private Pet findPetOrThrow(Long petId) {

        return petRepository.findById(petId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Pet not found: " + petId
                        )
                );
    }
}


