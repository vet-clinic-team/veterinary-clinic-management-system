package com.efe.veterinaryclinic.vet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VetRepository extends JpaRepository<Vet, Long> {

    boolean existsByLicenseNo(String licenseNo);

    boolean existsByLicenseNoAndIdNot(String licenseNo, Long id);

    List<Vet> findByActiveTrue();

    long countByActiveTrue();

    long countByCreatedAtGreaterThanEqual(LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT v.specialty) FROM Vet v")
    long countDistinctSpecialty();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vet v where v.id = :id")
    Optional<Vet> findByIdForUpdate(@Param("id") Long id);
}
