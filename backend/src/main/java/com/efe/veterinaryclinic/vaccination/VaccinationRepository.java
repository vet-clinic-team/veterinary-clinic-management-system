
package com.efe.veterinaryclinic.vaccination;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface VaccinationRepository extends JpaRepository<Vaccination, Long> {

    Page<Vaccination> findByPet_Id(Long petId, Pageable pageable);

    long countByNextDueDateLessThanEqual(LocalDate date);

    long countByNextDueDateAfter(LocalDate date);

    long countByAdministeredAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    List<Vaccination> findByNextDueDateLessThanEqualOrderByNextDueDateAsc(
            LocalDate date
    );

    List<Vaccination> findByNextDueDate(LocalDate date);

    // Count all vaccinations of the same type for a pet.
    long countByPet_IdAndVaccineTypeIgnoreCase(
            Long petId,
            String vaccineType
    );

    // Count vaccinations excluding a specific record.
    long countByPet_IdAndVaccineTypeIgnoreCaseAndIdNot(
            Long petId,
            String vaccineType,
            Long excludedId
    );

    // Count only vaccinations performed before the given date.
    long countByPet_IdAndVaccineTypeIgnoreCaseAndAdministeredAtBefore(
            Long petId,
            String vaccineType,
            LocalDateTime administeredAt
    );

    // Count previous vaccinations while excluding the current record.
    long countByPet_IdAndVaccineTypeIgnoreCaseAndAdministeredAtBeforeAndIdNot(
            Long petId,
            String vaccineType,
            LocalDateTime administeredAt,
            Long excludedId
    );

    // Retrieve vaccinations in chronological order.
    List<Vaccination> findByPet_IdAndVaccineTypeIgnoreCaseOrderByAdministeredAtAscIdAsc(
            Long petId,
            String vaccineType
    );

    @Query("SELECT COUNT(DISTINCT v.pet.id) FROM Vaccination v")
    long countDistinctPet();
    
    // Find pets with existing vaccinations of a specific type and species.
    @Query("""
        SELECT DISTINCT v.pet.id
        FROM Vaccination v
        WHERE UPPER(v.vaccineType) = UPPER(:vaccineType)
          AND UPPER(v.pet.species) = UPPER(:species)
        """)
    List<Long> findPetIdsByVaccineTypeAndSpecies(
            String vaccineType,
            String species
    );

}

