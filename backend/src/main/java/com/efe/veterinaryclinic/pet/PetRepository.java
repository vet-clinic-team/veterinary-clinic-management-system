package com.efe.veterinaryclinic.pet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface PetRepository extends JpaRepository<Pet, Long>, JpaSpecificationExecutor<Pet> {

    long countByOwnerId(Long ownerId);

    boolean existsByOwnerIdAndArchivedFalse(Long ownerId);

    long countByArchivedFalse();

    long countByArchivedFalseAndSpeciesIgnoreCase(String species);

    long countByCreatedAtGreaterThanEqual(LocalDateTime since);

    List<Pet> findByOwnerId(Long ownerId);

    List<Pet> findByCreatedAtGreaterThanEqual(LocalDateTime since);
}
