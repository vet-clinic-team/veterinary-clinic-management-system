package com.efe.veterinaryclinic.visit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VisitRepository extends JpaRepository<Visit, Long>, JpaSpecificationExecutor<Visit> {

    List<Visit> findByVet_IdAndStatusNotAndScheduledAtBetween(
            Long vetId, VisitStatus excludedStatus, LocalDateTime windowStart, LocalDateTime windowEnd);

    Page<Visit> findByPet_Id(Long petId, Pageable pageable);

    long countByStatusNotAndScheduledAtBetween(
            VisitStatus excludedStatus, LocalDateTime windowStart, LocalDateTime windowEnd);

    List<Visit> findByStatusNotAndScheduledAtBetween(
            VisitStatus excludedStatus, LocalDateTime windowStart, LocalDateTime windowEnd);

    List<Visit> findByStatusNotAndScheduledAtBetweenOrderByScheduledAtAsc(
            VisitStatus excludedStatus, LocalDateTime windowStart, LocalDateTime windowEnd);

    List<Visit> findByStatusAndFollowUpDateBeforeOrderByFollowUpDateAsc(
            VisitStatus status, LocalDate date);

    long countByVet_IdAndScheduledAtBetween(
            Long vetId, LocalDateTime windowStart, LocalDateTime windowEnd);

    long countByVet_IdAndStatusAndScheduledAtBetween(
            Long vetId, VisitStatus status, LocalDateTime windowStart, LocalDateTime windowEnd);

    long countByVet_IdAndStatusNotAndScheduledAtGreaterThanEqual(
            Long vetId, VisitStatus excludedStatus, LocalDateTime from);

    Optional<Visit> findTopByPet_IdAndStatusNotOrderByScheduledAtDesc(Long petId, VisitStatus excludedStatus);

    List<Visit> findByStatusAndScheduledAtBetweenOrderByScheduledAtAsc(
            VisitStatus status, LocalDateTime windowStart, LocalDateTime windowEnd);

    List<Visit> findByCreatedAtGreaterThanEqual(LocalDateTime since);

    List<Visit> findByStatusAndScheduledAtBetweenAndReminderSentAtIsNull(
            VisitStatus status, LocalDateTime windowStart, LocalDateTime windowEnd);
}
