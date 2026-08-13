package com.efe.veterinaryclinic.visit;

import com.efe.veterinaryclinic.common.dto.PageResponse;
import com.efe.veterinaryclinic.common.exception.ConflictException;
import com.efe.veterinaryclinic.common.exception.ResourceNotFoundException;
import com.efe.veterinaryclinic.pet.Pet;
import com.efe.veterinaryclinic.pet.PetRepository;
import com.efe.veterinaryclinic.security.Role;
import com.efe.veterinaryclinic.vet.Vet;
import com.efe.veterinaryclinic.vet.VetRepository;
import com.efe.veterinaryclinic.visit.dto.MedicalNotesUpdateRequest;
import com.efe.veterinaryclinic.visit.dto.VisitRequest;
import com.efe.veterinaryclinic.visit.dto.VisitResponse;
import com.efe.veterinaryclinic.visit.dto.VisitStatusUpdateRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VisitService {

    private static final long OVERLAP_WINDOW_MINUTES = 15;
    private static final int FOLLOW_UP_DEFAULT_HOUR = 9;
    private static final String FOLLOW_UP_CHIEF_COMPLAINT = "Follow-up visit";

    private final VisitRepository visitRepository;
    private final PetRepository petRepository;
    private final VetRepository vetRepository;

    public VisitService(VisitRepository visitRepository, PetRepository petRepository, VetRepository vetRepository) {
        this.visitRepository = visitRepository;
        this.petRepository = petRepository;
        this.vetRepository = vetRepository;
    }

    @Transactional
    public VisitResponse create(VisitRequest request) {
        Pet pet = findPetOrThrow(request.petId());
        Vet vet = findVetForUpdateOrThrow(request.vetId());
        checkNoOverlap(request.vetId(), request.scheduledAt(), null);

        Visit visit = new Visit(pet, vet, request.scheduledAt(), request.chiefComplaint());
        Visit saved = visitRepository.save(visit);

        return VisitResponse.from(saved);
    }

    public VisitResponse getById(Long id) {
        return VisitResponse.from(findVisitOrThrow(id));
    }

    public PageResponse<VisitResponse> list(Long vetId, LocalDate from, LocalDate to, VisitStatus status, Pageable pageable) {
        Specification<Visit> spec = buildFilterSpec(vetId, from, to, status);

        return PageResponse.from(visitRepository.findAll(spec, pageable).map(VisitResponse::from));
    }

    public PageResponse<VisitResponse> listByPet(Long petId, Pageable pageable) {
        findPetOrThrow(petId);

        return PageResponse.from(visitRepository.findByPet_Id(petId, pageable).map(VisitResponse::from));
    }

    public List<Visit> searchTop(String search, int limit) {
        return visitRepository.findAll(VisitSpecifications.matchesSearch(search),
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "scheduledAt"))).getContent();
    }

    public List<VisitResponse> calendar(Long vetId, LocalDate from, LocalDate to, VisitStatus status) {
        Specification<Visit> spec = buildFilterSpec(vetId, from, to, status);

        return visitRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "scheduledAt")).stream()
                .map(VisitResponse::from)
                .toList();
    }

    private Specification<Visit> buildFilterSpec(Long vetId, LocalDate from, LocalDate to, VisitStatus status) {
        Specification<Visit> spec = (root, query, cb) -> cb.conjunction();

        if (vetId != null) {
            spec = spec.and(VisitSpecifications.hasVetId(vetId));
        }
        if (from != null) {
            spec = spec.and(VisitSpecifications.scheduledAtFrom(from.atStartOfDay()));
        }
        if (to != null) {
            spec = spec.and(VisitSpecifications.scheduledAtBefore(to.plusDays(1).atStartOfDay()));
        }
        if (status != null) {
            spec = spec.and(VisitSpecifications.hasStatus(status));
        }

        return spec;
    }

    @Transactional
    public VisitResponse update(Long id, VisitRequest request) {
        Visit visit = findVisitOrThrow(id);
        Pet pet = findPetOrThrow(request.petId());
        Vet vet = findVetForUpdateOrThrow(request.vetId());
        checkNoOverlap(request.vetId(), request.scheduledAt(), id);

        visit.update(pet, vet, request.scheduledAt(), request.chiefComplaint());

        return VisitResponse.from(visitRepository.save(visit));
    }

    @Transactional
    public VisitResponse updateStatus(Long id, VisitStatusUpdateRequest request) {
        Visit visit = findVisitOrThrow(id);

        if (visit.getStatus() == VisitStatus.CANCELLED && request.status() != VisitStatus.CANCELLED) {
            findVetForUpdateOrThrow(visit.getVet().getId());
            checkNoOverlap(visit.getVet().getId(), visit.getScheduledAt(), id);
        }

        visit.updateStatus(request.status());

        return VisitResponse.from(visitRepository.save(visit));
    }

    public VisitResponse updateMedicalNotes(Long id, MedicalNotesUpdateRequest request, Role requesterRole) {
        if (requesterRole == Role.RECEPTIONIST) {
            throw new AccessDeniedException("RECEPTIONIST cannot edit treatment notes");
        }

        Visit visit = findVisitOrThrow(id);
        visit.updateMedicalNotes(request.diagnosis(), request.treatmentNotes(), request.followUpDate());

        return VisitResponse.from(visitRepository.save(visit));
    }

    @Transactional
    public VisitResponse createFollowUp(Long id, Role requesterRole) {
        if (requesterRole == Role.RECEPTIONIST) {
            throw new AccessDeniedException("RECEPTIONIST cannot create follow-up visits");
        }

        Visit visit = findVisitOrThrow(id);
        if (visit.getStatus() != VisitStatus.COMPLETED) {
            throw new ConflictException("Follow-up can only be created for a completed visit");
        }
        if (visit.getFollowUpDate() == null) {
            throw new ConflictException("Visit has no follow-up date set");
        }

        LocalDateTime scheduledAt = visit.getFollowUpDate().atTime(FOLLOW_UP_DEFAULT_HOUR, 0);
        findVetForUpdateOrThrow(visit.getVet().getId());
        checkNoOverlap(visit.getVet().getId(), scheduledAt, null);

        Visit followUpVisit = new Visit(visit.getPet(), visit.getVet(), scheduledAt, FOLLOW_UP_CHIEF_COMPLAINT);

        return VisitResponse.from(visitRepository.save(followUpVisit));
    }

    private void checkNoOverlap(Long vetId, LocalDateTime scheduledAt, Long excludeVisitId) {
        LocalDateTime windowStart = scheduledAt.minusMinutes(OVERLAP_WINDOW_MINUTES);
        LocalDateTime windowEnd = scheduledAt.plusMinutes(OVERLAP_WINDOW_MINUTES);

        boolean conflict = visitRepository
                .findByVet_IdAndStatusNotAndScheduledAtBetween(vetId, VisitStatus.CANCELLED, windowStart, windowEnd)
                .stream()
                .anyMatch(visit -> !visit.getId().equals(excludeVisitId));

        if (conflict) {
            throw new ConflictException(
                    "Vet already has an appointment within 15 minutes of " + scheduledAt);
        }
    }

    private Visit findVisitOrThrow(Long id) {
        return visitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found with id " + id));
    }

    private Pet findPetOrThrow(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with id " + petId));
    }

    private Vet findVetForUpdateOrThrow(Long vetId) {
        return vetRepository.findByIdForUpdate(vetId)
                .orElseThrow(() -> new ResourceNotFoundException("Vet not found with id " + vetId));
    }
}
